package com.github.johanrg.ast;

import com.github.johanrg.compiler.DataType;
import com.github.johanrg.compiler.Location;
import com.sun.corba.se.spi.oa.ObjectAdapter;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTVariable extends ASTLiteral implements Identifier {
    private final String identifier;
    private final boolean isConstant;

    public ASTVariable(String identifier, Object value, DataType dataType, boolean isConstant, Location location) {
        super(value, dataType, location);
        this.identifier = identifier;
        this.isConstant = isConstant;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    public boolean isConstant() {
        return isConstant;
    }

    @Override
    public String toString() {
        return String.format("%s: %s : %s", this.getClass().getName(), identifier, getDataType().toString());
    }
}
