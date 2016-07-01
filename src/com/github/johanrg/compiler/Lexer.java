package com.github.johanrg.compiler;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @author johan
 * @since 2016-06-29.
 */
public class Lexer {
    @FunctionalInterface
    private interface State {
        State lex();
    }

    private final char EOF = '\0';
    private final char[] WHITESPACE = new char[] {' ', '\t', '\r'};
    private final char NEW_LINE = '\n';
    private final char QUOTE = '"';
    private final char SINGLE_QUOTE = '\'';
    private final char[] DECIMAL = new char[]{'0', '9', '.', '.'};
    private final char[] HEXADECIMAL = new char[]{'0', '9', 'a', 'f', 'A', 'F'};
    private final char[] ALPHA = new char[]{'a', 'z', 'A', 'Z', '_', '_'};
    private final char[] ALPHANUMERIC = new char[]{'a', 'z', 'A', 'Z', '0', '9', '_', '_'};
    private final char[] DELIMITER = new char[] {',', ':', '[', ']', '{', '}', '(', ')'};
    private final char[] OPERATOR = new char[] {'+', '-', '*', '/', '!', '%', '&', '|', '=', '<', '>'};
    private final char[] SPECIAL_INSIDE_QUOTE = new char[] {'\\', '"', '\n'};
    private final char[] SPECIAL_INSIDE_SINGLE_QUOTE = new char[] {'\\', '\'', '\n'};

    private final Queue<Token> tokens = new LinkedList<>();
    private String fileName;
    private String source;
    private int start = 0;
    private int pos = 0;
    private int line = 1;
    private int column = 1;
    private Location location;

    public void lex(String fileName, String source) {
        this.fileName = fileName;
        this.source = source + EOF;
        for (State state = this::lexStart; state != null; state = state.lex()) ;
    }

    private State lexStart() {
        updateLocation();
        if (accept(WHITESPACE)) {
            return this::lexWhiteSpace;
        } else if (acceptRange(DECIMAL)) {
            return this::lexNumber;
        } else if (acceptRange(ALPHA)) {
            return this::lexIdentifier;
        } else if (accept(OPERATOR)) {
            return this::lexOperator;
        } else if (accept(DELIMITER)) {
           return this::lexDelimiter;
        } else if (check(QUOTE)) {
            return this::lexStartOfQuote;
        } else if (check(SINGLE_QUOTE)) {
            return this::lexStartOfSingleQuote;
        } else if (check(NEW_LINE)) {
            return this::lexNewLine;
        } else if (accept(EOF)) {
            return this::lexEndOfFile;
        } else {
            return error("syntax error");
        }
    }

    private State lexNewLine() {
        while (accept('\n')) {
            ++line;
            column = 1;
        }
        ignore();
        return this::lexStart;
    }

    private State lexLineComment() {
        while (notIn(NEW_LINE)) ;
        ignore();
        return this::lexStart;
    }

    private State lexMultiLineComment() {
        int nested = 1;
        for (; ;) {
            while (notIn('/', '*', '\n')) ;
            if (check('\n')) {
                lexNewLine();
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

    private State lexNumber() {
        char[] digits = DECIMAL;
        if (accept('0') && accept('x', 'X')) {
            digits = HEXADECIMAL;
        }
        batchAcceptRange(digits);
        if (accept('.')) {
            batchAcceptRange(digits);
        }
        if (accept('e', 'E')) {
            accept('+', '-');
            batchAcceptRange(DECIMAL);
        }
        accept('f', 'F');
        if (Character.isAlphabetic(peek())) {
            next();
            return error(String.format("bad number syntax %s", source.substring(start, pos)));
        }
        newToken(TokenType.LITERAL_NUMBER);
        return this::lexStart;
    }

    private State lexWhiteSpace() {
        while (accept(WHITESPACE)) ;
        ignore();
        return this::lexStart;
    }

    private State lexIdentifier() {
        while (acceptRange(ALPHANUMERIC)) ;
        newToken(TokenType.IDENTIFIER);
        return this::lexStart;
    }

    private State lexOperator() {
        while (accept(OPERATOR)) ;
        if (length() >= 2 && source.substring(start, pos).equals("//")) {
            return this::lexLineComment;
        } else if (length() >= 2 && source.substring(start, pos).equals("/*")) {
            return this::lexMultiLineComment;
        }
        newToken(TokenType.OPERATOR);
        return this::lexStart;
    }

    private State lexDelimiter() {
        newToken(TokenType.DELIMITER);
        return this::lexStart;
    }

    private State lexStartOfQuote() {
        skip();
        ignore();
        return this::lexInsideQuote;
    }

    private State lexInsideQuote() {
        do {
            while (notIn(SPECIAL_INSIDE_QUOTE)) ;

            if (accept(EOF, NEW_LINE)) {
                return error("illegal line end in string literal");
            }
            if (accept('\\')) {
                accept(QUOTE);
            }
        } while (notIn(QUOTE));

        newToken(TokenType.LITERAL_STRING);
        return this::lexEndOfQuote;
    }

    private State lexEndOfQuote() {
        skip();
        ignore();
        return this::lexStart;
    }

    private State lexStartOfSingleQuote() {
        skip();
        ignore();
        return this::lexInsideSingleQuote;
    }

    private State lexInsideSingleQuote() {
        do {
            while (notIn(SPECIAL_INSIDE_SINGLE_QUOTE)) ;

            if (accept(EOF, NEW_LINE)) {
                return error("illegal line end in char literal");
            }
            if (accept('\\')) {
                accept(SINGLE_QUOTE);
            }
        } while (notIn(SINGLE_QUOTE));

        if (length() != 1) {
            return error("illegal char literal");
        }
        newToken(TokenType.LITERAL_CHAR);
        return this::lexEndOfQuote;
    }

    private State lexEndOfFile() {
        return null;
    }

    private void updateLocation() {
        location = new Location(fileName, line, column);
    }

    private State error(String message) {
        System.out.printf("Error:(%d,%d) %s", location.getLine(), location.getColumn(), message);
        return null;
    }

    private void newToken(TokenType tokenType) {
        tokens.offer(new Token(tokenType, source.substring(start, pos), location));
        ignore();
    }

    private int length() {
        return pos - start;
    }

    private void ignore() {
        start = pos;
    }

    private void revert() {
        --pos;
        --column;
    }

    private void skip() {
        ++pos;
        ++column;
        ignore();
    }

    private char peek() {
        return source.charAt(pos);
    }

    private char next() {
        char c = source.charAt(pos++);
        ++column;
        return c;
    }

    private boolean accept(char... valid) {
        assert valid.length > 0 : "accept must have at least one argument";
        char c = next();
        for(char v : valid) {
            if (c == v) {
                return true;
            }
        }
        revert();
        return false;
    }

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

    private void batchAcceptRange(char... valid) {
        assert valid.length % 2 == 0 && valid.length > 0 : "must have 1 or more pair of arguments";
        while (acceptRange(valid)) ;
    }

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

    private boolean notIn(char... notValid) {
        char c = next();
        if (c == EOF) {
            revert();
            return false;
        }

        for (char n : notValid) {
            if (c == n) {
                revert();
                return false;
            }
        }
        return true;
    }

    public Queue<Token> getTokens() {
        return tokens;
    }
}
