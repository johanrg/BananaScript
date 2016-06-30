package com.github.johanrg.ast1;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTLiteral extends ASTNode {
    private final Value value;

    public ASTLiteral(Value value, Location location) {
        super(location);
        this.value = value;
    }

    public Value getValue() {
        return value;
    }
}
