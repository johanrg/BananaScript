package com.github.johanrg.ast1;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTUnaryOperator extends ASTOperator {
    private final ASTNode node;

    public ASTUnaryOperator(Type type, ASTNode node, Location location) {
        super(location, type);
        this.node = node;
    }

    public ASTNode getNode() {
        return node;
    }
}
