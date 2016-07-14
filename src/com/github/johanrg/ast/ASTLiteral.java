package com.github.johanrg.ast;

import com.github.johanrg.frontend.DataType;
import com.github.johanrg.frontend.Location;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTLiteral extends ASTNode implements Type {


    private final Object value;
    private final DataType dataType;

    public ASTLiteral(Object value, DataType dataType, Location location) {
        super(location);
        this.value = value;
        this.dataType = dataType;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    public static DataType typeForName(String name) {
        for (DataType t : DataType.values()) {
            if (t.toString().toLowerCase().equals(name)) {
                return t;
            }
        }
        return null;
    }

    public boolean getBoolean() {
        assert dataType == DataType.BOOLEAN : "Literal is not a boolean";
        return (Boolean) value;
    }

    public int getInt() {
        assert dataType == DataType.INT : "Literal is not an integer";
        return (Integer) value;
    }

    public float getFloat() {
        assert dataType == DataType.FLOAT : "Literal is not a float";
        return (Float) value;
    }

    public double getDouble() {
        assert dataType == DataType.DOUBLE : "Literal is not a double";
        return (Double) value;
    }

    public String getString() {
        assert dataType == DataType.STRING : "Literal is not a double";
        return (String) value;
    }

    public static Object defaultValueForType(DataType dataType) {
        switch (dataType) {
            case INT:
                return 0;
            case FLOAT:
                return 0.0f;
            case DOUBLE:
                return 0.0;
            case CHAR:
                return '\0';
            case STRING:
                return "";
            case VOID:
                return null;
            default:
                return null;
        }
    }
}
