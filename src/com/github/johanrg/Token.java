package com.github.johanrg;

/**
 * @author johan
 * @since 2016-06-29.
 */
public class Token {
    private final TokenType type;
    private final String data;

    public Token(TokenType type, String data) {
        this.type = type;
        this.data = data;
    }
}
