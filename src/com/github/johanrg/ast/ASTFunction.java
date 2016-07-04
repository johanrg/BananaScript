package com.github.johanrg.ast;

import com.github.johanrg.compiler.DataType;
import com.github.johanrg.compiler.Location;

import java.util.List;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTFunction extends ASTNode implements Identifier {
    private final String identifier;
    private final List<ASTNode> parameters;
    private final ASTNode statement;
    private final DataType returnDataType;

    public ASTFunction(String identifier, List<ASTNode> parameters, ASTNode statement, DataType returnDataType, Location location) {
        super(location);
        this.parameters = parameters;
        this.statement = statement;
        this.identifier = identifier;
        this.returnDataType = returnDataType;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<ASTNode> getParameters() {
        return parameters;
    }

    public ASTNode getStatement() {
        return statement;
    }

    public DataType getReturnDataType() {
        return returnDataType;
    }
}
