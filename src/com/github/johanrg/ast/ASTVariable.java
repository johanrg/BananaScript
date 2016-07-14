package com.github.johanrg.ast;

import com.github.johanrg.frontend.DataType;
import com.github.johanrg.frontend.Location;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTVariable extends ASTNode implements Identifier, Type {
    private final String identifier;
    private final DataType dataType;

    public ASTVariable(String identifier, DataType dataType, Location location) {
        super(location);
        this.identifier = identifier;
        this.dataType = dataType;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public String toString() {
        return String.format("%s: %s : %s", this.getClass().getName(), identifier, getDataType().toString());
    }
}
