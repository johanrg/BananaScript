package com.github.johanrg.compiler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * The class lexes source files into tokens.
 *
 * @author johan
 * @since 2016-06-29.
 */
public class Lexer {
    /**
     * Used for the functional state machine
     */
    @FunctionalInterface
    private interface State {
        State lex() throws CompilerException;
    }

    // Constants
    private static final char EOF = '\0';
    private static final char NEW_LINE = '\n';
    private static final char CITATION = '"';
    private static final char APOSTROPHE = '\'';
    private static final char BACKSLASH = '\\';
    private static final char[] WHITESPACE = new char[]{' ', '\t', '\r', '\b', '\f'};
    private static final char[] ESCAPED_CHAR = new char[]{'t', 'r', 'n', 'b', 'f'};
    private static final char[] DECIMAL_RANGE = new char[]{'0', '9'};
    private static final char[] HEXADECIMAL_RANGE = new char[]{'0', '9', 'a', 'f', 'A', 'F'};
    private static final char[] ALPHA_RANGE = new char[]{'a', 'z', 'A', 'Z', '_', '_'};
    private static final char[] ALPHANUMERIC_RANGE = new char[]{'a', 'z', 'A', 'Z', '0', '9', '_', '_'};
    private static final char[] DELIMITER = new char[]{',', ':', '[', ']', '{', '}', '(', ')', '.', ';', '@'};
    private static final char[] OPERATOR = new char[]{'+', '-', '*', '/', '!', '%', '&', '|', '=', '<', '>', '^'};

    private final List<Token> tokens = new LinkedList<>();
    private final List<String> errors = new ArrayList<>();
    private String fileName;
    private String source;
    private int start;
    private int pos;
    private int line;
    private int column;
    private Location location;
    private Token.Type tokenType;
    private int scopeLevel = 0;
    private boolean writtenToken = false;

    /**
     * WIP params will change.
     *
     * @param fileName
     * @param source
     */
    public void lex(String fileName, String source) throws CompilerException {
        this.fileName = fileName;
        this.source = source + EOF;
        start = 0;
        pos = 0;
        line = 1;
        column = 1;

        // The functional state machine loop
        for (State state = this::lexStart; state != null; state = state.lex()) ;
    }

    /**
     * The main state of the state machine.
     *
     * @return A new functional State.
     */
    private State lexStart() throws CompilerException {
        updateLocation();
        if (check(WHITESPACE)) {
            return this::handleWhiteSpaces;
        } else if (match("//")) {
            return this::handleLineComment;
        } else if (match("/*")) {
            return this::handleMultiLineComment;
        } else if (check(NEW_LINE)) {
            return this::handleNewLine;
        } else if (checkRange(DECIMAL_RANGE)) {
            return this::lexDecimalNumber;
        } else if (acceptRange(ALPHA_RANGE)) {
            return this::lexIdentifier;
        } else if (accept(OPERATOR)) {
            return this::lexOperator;
        } else if (accept(DELIMITER)) {
            return this::lexDelimiter;
        } else if (accept(CITATION)) {
            return this::lexString;
        } else if (accept(APOSTROPHE)) {
            return this::lexChar;

        } else if (accept(EOF)) {
            return this::handleEndOfFile;
        } else {
            error("syntax error");
        }
        return null;
    }

    /**
     * Handles white spaces
     *
     * @return A new functional state.
     */
    private State handleWhiteSpaces() throws CompilerException {
        if (column == 1) {
            int spaces = 0;
            int tabs = 0;
            while (accept(' ')) {
                ++spaces;
            }
            while (accept('\t')) {
                ++tabs;
            }
            if (tabs > 0 && spaces > 0) {
                error("mixed tabs and spaces");
            }
            if (spaces > 0) {
                if (spaces % 4 != 0) {
                    error("indentation spaces must be in multiples of 4");
                }
                scopeLevel = spaces / 4;
            } else if (tabs > 0) {
                scopeLevel = tabs;
            }
        }
        while (accept(WHITESPACE)) ;
        ignore();
        return this::lexStart;
    }

    /**
     * Handles new lines.
     *
     * @return a new functional state.
     */
    private State handleNewLine() throws CompilerException {
        if (accept(NEW_LINE)) {
            if (writtenToken) {
                newToken(Token.Type.END_OF_STATEMENT);
            } else {
                ignore();
            }
            writtenToken = false;
            ++line;
            column = 1;
            scopeLevel = 0;
        }
        return this::lexStart;
    }

    /**
     * Handles line commens //
     *
     * @return a new functional state.
     */
    private State handleLineComment() throws CompilerException {
        while (not(NEW_LINE)) ;
        ignore();
        return this::lexStart;
    }

    /**
     * Handles multi line comments, even if nested in other multi line comments.
     *
     * @return A new functional state
     */
    private State handleMultiLineComment() throws CompilerException {
        int nested = 0;
        for (; ; ) {
            while (not('/', '*', NEW_LINE)) ;
            if (check(NEW_LINE)) {
                handleNewLine();
            } else if (check(EOF)) {
                error("unclosed comment");
            } else if (accept('*') && accept('/') && --nested == 0) {
                break;
            } else if (accept('/') && accept('*')) {
                ++nested;
            }
        }
        ignore();
        return this::lexStart;
    }

    /**
     * Handles end of file.
     *
     * @return A new functional state.
     */
    private State handleEndOfFile() {
        newToken(Token.Type.EOF);
        return null;
    }

    private State lexDecimalNumber() throws CompilerException {
        if (match("0x") || match("0X")) {
            skip();
            skip();
            lexHexadecimalNumber();
            addLiteralHex();
            return this::lexStart;
        } else {
            lexUnsignedDigitSequence(false);
            if (accept('.')) {
                lexUnsignedDigitSequence(true);
            } else {
                addLiteral(DataType.INT);
                return this::lexStart;
            }
        }
        lexScaleFactor();
        if (accept('f', 'F')) {
            addLiteral(DataType.FLOAT);
        } else {
            addLiteral(DataType.DOUBLE);
        }
        return this::lexStart;
    }

    private void lexUnsignedDigitSequence(boolean optional) throws CompilerException {
        if (acceptRange(DECIMAL_RANGE)) {
            while (acceptRange(DECIMAL_RANGE)) ;
        } else if (!optional) {
            error("expexted digit");
        }
    }

    private void lexHexadecimalNumber() throws CompilerException {
        if (acceptRange(HEXADECIMAL_RANGE)) {
            while (acceptRange(HEXADECIMAL_RANGE)) ;
        } else {
            error("expected hexadecimal digit");
        }
    }

    private void lexScaleFactor() throws CompilerException {
        if (accept('e', 'E')) {
            accept('+', '-');
            lexUnsignedDigitSequence(true);
        }
    }

    private void addLiteralHex() throws CompilerException {
        if (Character.isAlphabetic(peek())) {
            error("bad number syntax");
        }
        String value = source.substring(start, pos);
        try {
            value = Integer.toString(Integer.parseInt(value, 16));
        } catch (NumberFormatException e) {
            error("bad hexadecimal syntax");
        }
        tokens.add(new Token(Token.Type.LITERAL, DataType.INT, value, scopeLevel, location));
        ignore();
    }

    private void addLiteral(DataType dataType) throws CompilerException {
        if (Character.isAlphabetic(peek())) {
            error("bad number syntax");
        }
        tokens.add(new Token(Token.Type.LITERAL, dataType, source.substring(start, pos), scopeLevel, location));
        ignore();
    }

    /**
     * Lexes identifiers including keywords.
     *
     * @return A new functional state.
     */
    private State lexIdentifier() {
        while (acceptRange(ALPHANUMERIC_RANGE)) ;
        Symbols.Keyword keyword = Symbols.match(source.substring(start, pos));
        if (keyword == null) {
            newToken(Token.Type.IDENTIFIER);
        } else {
            newToken(Token.Type.KEYWORD, keyword);
        }
        return this::lexStart;
    }

    /**
     * Lexes operators
     *
     * @return A new functional state.
     */
    private State lexOperator() {
        while (accept(OPERATOR)) ;
        newToken(Token.Type.OPERATOR);
        return this::lexStart;
    }

    /**
     * Lexes delimiters like , : () {} etc.
     *
     * @return A new functional state.
     */
    private State lexDelimiter() {
        newToken(Token.Type.DELIMITER);
        return this::lexStart;
    }

    /**
     * Lexes string literal "..."
     *
     * @return A new functional state.
     */
    private State lexString() throws CompilerException {
        ignore();
        do {
            while (not(BACKSLASH, CITATION, NEW_LINE)) ;

            if (accept(EOF, NEW_LINE)) {
                error("illegal line end in string literal");
            }
            if (accept(BACKSLASH)) {
                accept(CITATION);
            }
        } while (not(CITATION));

        addLiteral(DataType.STRING);

        skip();
        ignore();
        return this::lexStart;
    }

    /**
     * Lexes char literal ' ' but does not verify that it only contains one character.
     *
     * @return A new functional state.
     */
    private State lexChar() throws CompilerException {
        int size = 0;
        ignore();
        do {
            while (not(BACKSLASH, APOSTROPHE, NEW_LINE)) {
                ++size;
            }

            if (accept(BACKSLASH)) {
                if (accept(ESCAPED_CHAR)) {
                    ++size;
                } else if (acceptRange(DECIMAL_RANGE)) {
                    acceptRange(DECIMAL_RANGE);
                    acceptRange(DECIMAL_RANGE);
                    ++size;
                } else {
                    error("illegal escape code");
                }
            }
            if (accept(EOF, NEW_LINE)) {
                error("illegal line end in char literal");
            }
        } while (!check(APOSTROPHE));

        if (size > 1) {
            error("illegal literal character length");
        }
        addLiteral(DataType.CHAR);
        skip();
        ignore();
        return this::lexStart;
    }

    /**
     * Updates the location object to the current position in the source file, to set the token to the correct
     * line and column in newToken.
     */
    private void updateLocation() {
        location = new Location(fileName, line, column);
    }

    /**
     * Error handling in the lexer
     *
     * @param error The error message
     */
    private void error(String error) throws CompilerException {
        throw new CompilerException(String.format("Error:(%d,%d) %s (%s)", location.getLine(), location.getColumn(), error, location.getFileName()));
    }

    private void addToken(Token token) {
        tokens.add(token);
        ignore();
        writtenToken = true;
    }

    /**
     * Creates a new token with the current start to pos position data from the source file.
     *
     * @param type The type of token to be created.
     */
    private void newToken(Token.Type type) {
        addToken(new Token(type, source.substring(start, pos), scopeLevel, location));
    }

    private void newToken(Token.Type type, Symbols.Keyword keyword) {
        addToken(new Token(type, keyword, source.substring(start, pos), scopeLevel, location));
    }

    private String save() {
        String s = source.substring(start, pos);
        ignore();
        return s;
    }

    /**
     * Sets the source start to current position, ignoring everything before it.
     */
    private void ignore() {
        start = pos;
    }

    /**
     * Backs up the position one step.
     */
    private void backup() {
        --pos;
        if (start > pos) {
            start = pos;
        }
        --column;
    }

    /**
     * Skips the current source position.
     */
    private void skip() {
        ++pos;
        ++column;
        ignore();
    }

    /**
     * Peeks at the current source position without moving the position forward.
     *
     * @return the current char at the source position.
     */
    private char peek() {
        return source.charAt(pos);
    }

    /**
     * Returns the char at the current position and moves the position forward one char step.
     *
     * @return the current char at the source position.
     */
    private char next() {
        char c = source.charAt(pos++);
        ++column;
        return c;
    }

    /**
     * Accepts the char at the current position if it's in the valid set and moves the position forward one step.
     *
     * @param valid Set of valid characters
     * @return boolean true if the character is valid.
     */
    private boolean accept(char... valid) {
        assert valid.length > 0 : "must have at least one argument";
        char c = next();
        for (char v : valid) {
            if (c == v) {
                return true;
            }
        }
        backup();
        return false;
    }

    /**
     * Accepts the char at the current position if it's in the valid range and moves the position forward one step.
     *
     * @param valid 1 or more pair of from char - to char ranges in relation to its ascii value.
     * @return boolean true if the character is valid.
     */
    private boolean acceptRange(char... valid) {
        assert valid.length % 2 == 0 && valid.length > 0 : "must have 1 or more pair of arguments";
        char c = next();
        for (int i = 0; i < valid.length - 1; i += 2) {
            if (c >= valid[i] && c <= valid[i + 1]) {
                return true;
            }
        }
        backup();
        return false;
    }

    /**
     * Checks if the character is in the valid set without moving the source position forward.
     *
     * @param valid Set of valid characters
     * @return boolean true if the character is valid.
     */
    private boolean check(char... valid) {
        assert valid.length > 0 : "must have at least one argument";
        char c = peek();
        for (char v : valid) {
            if (c == v) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the character is in the valid range without moving the source position forward.
     *
     * @param valid 1 or more pair of from char - to char ranges in relation to its ascii value.
     * @return boolean true if the character is valid.
     */
    private boolean checkRange(char... valid) {
        assert valid.length % 2 == 0 && valid.length > 0 : "must have 1 or more pair of arguments";
        char c = peek();
        for (int i = 0; i < valid.length - 1; i += 2) {
            if (c >= valid[i] && c <= valid[i + 1]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Accepts the char in the current positon if it's NOT in the set of characters.
     * Returns false on EOF.
     *
     * @param invalid Set of invalid characters
     * @return boolean true if it's not in the set.
     */
    private boolean not(char... invalid) {
        char c = next();
        if (c == EOF) {
            backup();
            return false;
        }

        for (char n : invalid) {
            if (c == n) {
                backup();
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the string matches a sequence of characters from the current position and forward but doesn't move
     * the source position forward.
     *
     * @param valid the valid string to match against
     * @return boolean true if there is a match.
     */
    private boolean match(String valid) {
        return (source.length() - start - 1 >= valid.length() && source.substring(start, start + valid.length()).equals(valid));
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<Token> getTokens() {
        return tokens;
    }
}
