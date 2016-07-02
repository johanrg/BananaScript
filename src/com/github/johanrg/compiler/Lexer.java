package com.github.johanrg.compiler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
        State lex();
    }

    // Constants
    private final char EOF = '\0';
    private final char NEW_LINE = '\n';
    private final char CITATION = '"';
    private final char APOSTROPHE = '\'';
    private final char BACKSLASH = '\\';
    private final char[] WHITESPACE = new char[]{' ', '\t', '\r'};
    private final char[] DECIMAL_RANGE = new char[]{'0', '9'};
    private final char[] HEXADECIMAL_RANGE = new char[]{'0', '9', 'a', 'f', 'A', 'F'};
    private final char[] ALPHA_RANGE = new char[]{'a', 'z', 'A', 'Z', '_', '_'};
    private final char[] ALPHANUMERIC_RANGE = new char[]{'a', 'z', 'A', 'Z', '0', '9', '_', '_'};
    private final char[] DELIMITER = new char[]{',', ':', '[', ']', '{', '}', '(', ')', '.', ';', '@'};
    private final char[] OPERATOR = new char[]{'+', '-', '*', '/', '!', '%', '&', '|', '=', '<', '>'};

    private final Queue<Token> tokens = new LinkedList<>();
    private final List<String> errors = new ArrayList<>();
    private String fileName;
    private String source;
    private int start;
    private int pos;
    private int line;
    private int column;
    private Location location;

    /**
     * WIP params will change.
     *
     * @param fileName
     * @param source
     */
    public void lex(String fileName, String source) {
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
    private State lexStart() {
        updateLocation();
        if (accept(WHITESPACE)) {
            return this::handleWhiteSpace;
        } else if (match("//")) {
            return this::handleLineComment;
        } else if (match("/*")) {
            return this::handleMultiLineComment;
        } else if (check(NEW_LINE)) {
            return this::handleNewLine;
        } else if (acceptRange(DECIMAL_RANGE)) {
            return this::lexNumber;
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
            return error("syntax error");
        }
    }

    /**
     * Handles white spaces
     *
     * @return A new functional state.
     */
    private State handleWhiteSpace() {
        while (accept(WHITESPACE)) ;
        ignore();
        return this::lexStart;
    }

    /**
     * Handles new lines.
     *
     * @return a new functional state.
     */
    private State handleNewLine() {
        while (accept(NEW_LINE)) {
            ++line;
            column = 1;
        }
        ignore();
        return this::lexStart;
    }

    /**
     * Handles line commens //
     *
     * @return a new functional state.
     */
    private State handleLineComment() {
        while (not(NEW_LINE)) ;
        ignore();
        return this::lexStart;
    }

    /**
     * Handles multi line comments, even if nested in other multi line comments.
     *
     * @return A new functional state
     */
    private State handleMultiLineComment() {
        int nested = 0;
        for (; ; ) {
            while (not('/', '*', NEW_LINE)) ;
            if (check(NEW_LINE)) {
                handleNewLine();
            } else if (check(EOF)) {
                return error("unclosed comment");
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
     * Handles end of file, very simple atm.
     *
     * @return A new functional state.
     */
    private State handleEndOfFile() {
        return null;
    }

    /**
     * Lexes number literal of type int, float, double, hexadecimal and exponential but does not verify that
     * the syntax of the number. Values like 0x.ef is dealt with in the parser.
     *
     * @return A new functional state.
     */
    private State lexNumber() {
        char[] digits = DECIMAL_RANGE;
        if (accept('0') && accept('x', 'X')) {
            digits = HEXADECIMAL_RANGE;
        }
        while (acceptRange(digits)) ;
        if (accept('.')) {
            while (acceptRange(digits)) ;
        }
        if (accept('e', 'E')) {
            accept('+', '-');
            while (acceptRange(DECIMAL_RANGE)) ;
        }
        accept('f', 'F');
        if (Character.isAlphabetic(peek())) {
            next();
            return error(String.format("bad number syntax %s", source.substring(start, pos)));
        }
        newToken(Token.Type.LITERAL_NUMBER);
        return this::lexStart;
    }

    /**
     * Lexes identifiers including keywords.
     *
     * @return A new functional state.
     */
    private State lexIdentifier() {
        while (acceptRange(ALPHANUMERIC_RANGE)) ;
        newToken(Token.Type.IDENTIFIER);
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
     * @return  A new functional state.
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
    private State lexString() {
        ignore();
        do {
            while (not(BACKSLASH, CITATION, NEW_LINE)) ;

            if (accept(EOF, NEW_LINE)) {
                return error("illegal line end in string literal");
            }
            if (accept(BACKSLASH)) {
                accept(CITATION);
            }
        } while (not(CITATION));

        newToken(Token.Type.LITERAL_STRING);

        skip();
        ignore();
        return this::lexStart;
    }

    /**
     * Lexes char literal ' ' but does not verify that it only contains one character.
     *
     * @return A new functional state.
     */
    private State lexChar() {
        ignore();
        do {
            while (not(BACKSLASH, APOSTROPHE, NEW_LINE)) ;

            if (accept(EOF, NEW_LINE)) {
                return error("illegal line end in char literal");
            }
            if (accept(BACKSLASH)) {
                accept(APOSTROPHE);
            }
        } while (not(APOSTROPHE));

        newToken(Token.Type.LITERAL_CHAR);
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
     * @return A new functional state
     */
    private State error(String error) {
        errors.add(String.format("Error:(%d,%d) %s (%s)", location.getLine(), location.getColumn(), error, location.getFileName()));
        return null;
    }

    /**
     * Creates a new token with the current start to pos position data from the source file.
     *
     * @param type The type of token to be created.
     */
    private void newToken(Token.Type type) {
        tokens.offer(new Token(type, source.substring(start, pos), location));
        ignore();
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
    private void revert() {
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
        revert();
        return false;
    }

    /**
     * Accepts the char at the current position if it's in the valid range and moves the position forward one step.
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
        revert();
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
     * Accepts the char in the current positon if it's NOT in the set of characters.
     *
     * @param invalid Set of invalid characters
     * @return boolean true if it's not in the set.
     */
    private boolean not(char... invalid) {
        char c = next();
        if (c == EOF) {
            revert();
            return false;
        }

        for (char n : invalid) {
            if (c == n) {
                revert();
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

    public Queue<Token> getTokens() {
        return tokens;
    }
}
