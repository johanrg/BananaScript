package com.github.johanrg.ast1;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class Value<T> {
    enum Type {
        NONE,
        INTEGER,
        FLOAT,
        DOUBLE,
        CHAR,
        STRING;
    }
    private final T value;

    public Value(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public Type getType() {
        return Type.NONE;
    }
}
