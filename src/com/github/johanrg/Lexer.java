package com.github.johanrg;

import com.github.johanrg.ast1.Location;

/**
 * @author johan
 * @since 2016-06-29.
 */
public class Lexer {
    @FunctionalInterface
    private interface State {
        State lex();
    }

    private final String fileName;
    private final String source;
    private final char EOF = '\0';
    private int start = 0;
    private int pos = 0;
    private int line = 1;
    private int column = 1;
    private Location location;

    public Lexer(String fileName, String source) {
        this.fileName = fileName;
        this.source = source + EOF;
        updateLocation();
        for (State state = this::lexSource; state != null; state = state.lex()) ;
    }

    private State lexSource() {
        char c = next();

        if (c == ' ' || c == '\t' || c == '\r') {
            return this::lexWhiteSpace;
        } else if (c == '\n') {
            revert();
            return this::lexNewLine;
        } else if (c >= '0' && c <= '9') {
            revert();
            return this::lexNumber;
        } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
            return this::lexIdentifier;
        } else if (c == '"') {
            revert();
            return this::lexStartOfQuote;
        }

        return null;
    }

    private State lexNewLine() {
        while (accept("\n")) {
            ++line;
            column = 1;
        }
        return this::lexSource;
    }

    private State lexNumber() {
        accept("+-");
        String digits = "0123456789";
        if (accept("0") && accept("xX")) {
            digits = "0123456789abcdefABCDEF";
        }
        acceptUntilFail(digits);
        if (accept(".")) {
            acceptUntilFail(digits);
        }
        if (accept("eE")) {
            accept("+-");
            acceptUntilFail("0123456789");
        }
        accept("fF");
        if (Character.isAlphabetic(peek())) {
            next();
            return error(String.format("bad number syntax %s", source.substring(start, pos)));
        }
        newToken(TokenType.LITERAL_NUMBER);
        return this::lexSource;
    }

    private State lexWhiteSpace() {
        while (accept(" \t\r")) ;
        ignore();
        return this::lexSource;
    }

    private State lexIdentifier() {
        char c = next();
        while ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_') {
            c = next();
        }
        revert();
        newToken(TokenType.IDENTIFIER);
        return this::lexSource;
    }

    private State lexStartOfQuote() {
        skip();
        ignore();
        return this::lexInsideQuote;
    }

    private State lexInsideQuote() {
        do {
            char c = next();
            while (c != '\\' && c != '"' && c != '\n' && c != EOF) {
                c = next();
            }
            revert();
            if (c == EOF || c == ('\n')) {
                return error("Illegal line end in string literal");
            }
            if (accept("\\")) {
                accept("\"");
            }
        } while (!accept("\""));
        revert();
        return this::lexEndOfQuote;
    }

    private State lexEndOfQuote() {
        newToken(TokenType.LITERAL_STRING);
        skip();
        ignore();
        return this::lexSource;
    }

    private void updateLocation() {
        location = new Location(fileName, line, column);
    }

    private State error(String message) {
        System.out.printf("%s %d:%d", message, location.getLine(), location.getColumn());
        return null;
    }

    private void newToken(TokenType tokenType) {
        Token t = new Token(tokenType, source.substring(start, pos));
        System.out.print(tokenType);
        System.out.println(" '" + source.substring(start, pos) + "'");
        ignore();
    }

    private void ignore() {
        start = pos;
        updateLocation();
    }

    private void revert() {
        // NEVER revert back to add line break, this WILL fuck up the line/column counting
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

    private boolean accept(String valid) {
        if (valid.indexOf(next()) >= 0) {
            return true;
        }
        revert();
        return false;
    }

    private void acceptUntilFail(String valid) {
        while (accept(valid)) ;
    }
}
