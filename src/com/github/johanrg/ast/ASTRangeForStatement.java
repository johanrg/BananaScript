package com.github.johanrg.ast;

import com.github.johanrg.compiler.Location;

/**
 * @author Johan Gustafsson
 * @since 7/12/2016.
 */
public class ASTRangeForStatement extends ASTNode {
    private final ASTNode fromRange;
    private final ASTNode toRange;
    private final ASTNode forScope;

    public ASTRangeForStatement(ASTNode fromRange, ASTNode toRange, ASTNode forScope, Location location) {
        super(location);
        this.fromRange = fromRange;
        this.toRange = toRange;
        this.forScope = forScope;
    }

    public ASTNode getFromRange() {
        return fromRange;
    }

    public ASTNode getToRange() {
        return toRange;
    }

    public ASTNode getForScope() {
        return forScope;
    }
}
