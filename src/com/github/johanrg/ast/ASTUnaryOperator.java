package com.github.johanrg.ast;

import com.github.johanrg.compiler.Location;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTUnaryOperator extends ASTOperator {
    private final ASTNode singleNode;

    public ASTUnaryOperator(Type type, ASTNode singleNode, Location location) {
        super(type, location);
        this.singleNode = singleNode;
    }

    public ASTNode getSingleNode() {
        return singleNode;
    }
}
