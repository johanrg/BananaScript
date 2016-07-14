package com.github.johanrg.ast;

import com.github.johanrg.frontend.Location;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTBinaryOperator extends ASTOperator {
    private final ASTNode left;
    private final ASTNode right;

    public ASTBinaryOperator(Type type, ASTNode left, ASTNode right, Location location) {
        super(type, location);
        this.left = left;
        this.right = right;
    }

    public ASTNode getLeft() {
        return left;
    }

    public ASTNode getRight() {
        return right;
    }
}
