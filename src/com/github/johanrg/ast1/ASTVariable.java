package com.github.johanrg.ast1;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTVariable extends ASTLiteral implements Identifier {
    private final String identifier;

    public ASTVariable(String identifier, Value value, Location location) {
        super(value, location);
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }
}
