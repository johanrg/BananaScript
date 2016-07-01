package com.github.johanrg.compiler;

/**
 * @author johan
 * @since 2016-06-29.
 */
public class Token {
    private final TokenType type;
    private final String data;
    private final Location location;

    public Token(TokenType type, String data, Location location) {
        this.type = type;
        this.data = data;
        this.location = location;
    }

    public TokenType getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return String.format("(%d:%d) %s '%s'", location.getLine(), location.getColumn(), type.toString(), data) ;
    }
}
