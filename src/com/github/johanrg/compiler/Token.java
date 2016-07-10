package com.github.johanrg.compiler;

/**
 * @author johan
 * @since 2016-06-29.
 */
public class Token {
    public enum Type {
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
    private final DataType dataType;
    private final int scopeLevel;

    public Token(Type type, String data, int scopeLevel, Location location) {
        this.type = type;
        this.data = data;
        this.location = location;
        this.scopeLevel = scopeLevel;
        this.dataType = null;
    }

    public Token(Type type, DataType dataType, String data, int scopeLevel, Location location) {
        this.type = type;
        this.dataType = dataType;
        this.data = data;
        this.scopeLevel = scopeLevel;
        this.location = location;
    }

    public Type getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public DataType getDataType() {
        return dataType;
    }

    public int getScopeLevel() {
        return scopeLevel;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return String.format("(%d:%d) [%d] %s '%s'", location.getLine(), location.getColumn(), scopeLevel, type.toString(), data) ;
    }
}
