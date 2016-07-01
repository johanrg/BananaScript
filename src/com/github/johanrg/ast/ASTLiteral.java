package com.github.johanrg.ast;

import com.github.johanrg.compiler.Location;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTLiteral<T> extends ASTNode {
    public enum Type {
        INTEGER,
        FLOAT,
        DOUBLE,
        CHAR,
        STRING;
    }
    private final T value;

    public ASTLiteral(T value, Location location) {
        super(location);
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public Type getType() {
        if (value instanceof Integer) {
            return Type.INTEGER;
        } else if (value instanceof Float) {
            return Type.FLOAT;
        } else if (value instanceof Double) {
            return Type.DOUBLE;
        } else if (value instanceof Character) {
            return Type.CHAR;
        } else if (value instanceof String) {
            return Type.STRING;
        } else {
            assert false : "Unsupported type";
            return null;
        }
    }
}
