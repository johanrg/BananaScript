package com.github.johanrg.ast1;

import java.util.List;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTFunction extends ASTNode implements Identifier {
    private final String identifier;
    private final List<ASTNode> parameters;
    private final ASTNode statement;
    private final Value.Type returnType;

    public ASTFunction(String identifier, List<ASTNode> parameters, ASTNode statement, Value.Type returnType, Location location) {
        super(location);
        this.parameters = parameters;
        this.statement = statement;
        this.identifier = identifier;
        this.returnType = returnType;
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

    public Value.Type getReturnType() {
        return returnType;
    }
}
