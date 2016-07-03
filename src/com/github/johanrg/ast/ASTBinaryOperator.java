package com.github.johanrg.ast;

import com.github.johanrg.compiler.Location;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTBinaryOperator extends ASTOperator {
    private final ASTNode left;
    private final ASTNode right;

    public ASTBinaryOperator(Type type, ASTNode left, ASTNode right) {
        super(type);
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
