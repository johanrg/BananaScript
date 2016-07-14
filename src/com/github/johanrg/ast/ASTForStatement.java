package com.github.johanrg.ast;

import com.github.johanrg.frontend.Location;

/**
 * @author Johan Gustafsson
 * @since 7/12/2016.
 */
public class ASTForStatement extends ASTNode {
    private final ASTNode init;
    private final ASTNode condition;
    private final ASTNode increment;
    private final ASTScope forScope;

    public ASTForStatement(ASTNode init, ASTNode condition, ASTNode increment, ASTScope forScope, Location location) {
        super(location);
        this.init = init;
        this.condition = condition;
        this.increment = increment;
        this.forScope = forScope;
    }

    public ASTNode getInit() {
        return init;
    }

    public ASTNode getCondition() {
        return condition;
    }

    public ASTNode getIncrement() {
        return increment;
    }
}
