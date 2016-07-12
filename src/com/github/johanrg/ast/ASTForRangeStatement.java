package com.github.johanrg.ast;

import com.github.johanrg.compiler.Location;

/**
 * @author Johan Gustafsson
 * @since 7/12/2016.
 */
public class ASTForRangeStatement extends ASTNode {
    private final ASTNode range;
    private final ASTNode forScope;

    public ASTForRangeStatement(ASTNode range, ASTNode forScope, Location location) {
        super(location);
        this.range = range;
        this.forScope = forScope;
    }

    public ASTNode getRange() {
        return range;
    }

    public ASTNode getForScope() {
        return forScope;
    }
}
