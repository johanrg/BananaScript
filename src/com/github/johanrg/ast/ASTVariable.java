package com.github.johanrg.ast;

import com.github.johanrg.compiler.DataType;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTVariable extends ASTLiteral implements Identifier {
    private final String identifier;

    public ASTVariable(String identifier, Object value, DataType dataType) {
        super(value, dataType);
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return String.format("ASTVariable: %s : %s", identifier, getDataType().toString());
    }
}
