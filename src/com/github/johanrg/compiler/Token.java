package com.github.johanrg.compiler;

/**
 * @author johan
 * @since 2016-06-29.
 */
public class Token {
    enum Type {
        KEYWORD,
        LITERAL,
        OPERATOR,
        DELIMITER,
        IDENTIFIER,
        END_OF_STATEMENT,
        EOF;
    }

    private final Type type;
    private final String data;
    private final Location location;
    private final Symbols.Keyword keyword;
    private final DataType dataType;
    private final int scopeLevel;

    Token(Type type, String data, int scopeLevel, Location location) {
        this.type = type;
        this.keyword = null;
        this.data = data;
        this.location = location;
        this.scopeLevel = scopeLevel;
        this.dataType = null;
    }

    Token(Type type, DataType dataType, String data, int scopeLevel, Location location) {
        this.type = type;
        this.keyword = null;
        this.dataType = dataType;
        this.data = data;
        this.scopeLevel = scopeLevel;
        this.location = location;
    }

    Token(Type type, Symbols.Keyword keyword, String data, int scopeLevel, Location location) {
        this.type = type;
        this.keyword = keyword;
        this.dataType = null;
        this.data = data;
        this.scopeLevel = scopeLevel;
        this.location = location;
    }

    Type getType() {
        return type;
    }

    String getData() {
        return data;
    }

    public Symbols.Keyword getKeyword() {
        return keyword;
    }

    DataType getDataType() {
        return dataType;
    }

    int getScopeLevel() {
        return scopeLevel;
    }

    Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return String.format("(%d:%d) [%d] %s '%s'", location.getLine(), location.getColumn(), scopeLevel, type.toString(), data);
    }
}
