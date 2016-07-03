package com.github.johanrg.ast;

import com.github.johanrg.compiler.DataType;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTLiteral implements ASTNode {


    private final Object value;
    private final DataType dataType;

    public ASTLiteral(Object value, DataType dataType) {
        this.value = value;
        this.dataType = dataType;
    }

    public Object getValue() {
        return value;
    }

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

    public static Object defaultValueForType(DataType dataType) {
        switch (dataType) {
            case INT:
                return new Integer(0);
            case FLOAT:
                return new Float(0.0);
            case DOUBLE:
                return new Double(0.0);
            case CHAR:
                return new Character('\0');
            case STRING:
                return new String("");
            case VOID:
                return null;
            default:
                return null;
        }
    }
}
