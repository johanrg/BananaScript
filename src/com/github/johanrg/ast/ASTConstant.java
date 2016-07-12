package com.github.johanrg.ast;

import com.github.johanrg.compiler.DataType;
import com.github.johanrg.compiler.Location;

/**
 * @author Johan Gustafsson
 * @since 7/12/2016.
 */
public class ASTConstant extends ASTNode implements Identifier, Type {
    private final String identifier;
    private final DataType dataType;

    public ASTConstant(String identifier, DataType dataType, Location location) {
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
