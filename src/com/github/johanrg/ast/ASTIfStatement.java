package com.github.johanrg.ast;

import com.github.johanrg.compiler.Location;

/**
 * @author Johan Gustafsson
 * @since 7/11/2016.
 */
public class ASTIfStatement extends ASTNode {
    private final ASTNode expression;
    private final ASTScope ifScope;
    private final ASTScope elseScope;

    public ASTIfStatement(ASTNode expression, ASTScope ifScope, ASTScope elseScope, Location location ) {
        super(location);
        this.expression = expression;
        this.ifScope = ifScope;
        this.elseScope = elseScope;
    }

    public ASTNode getExpression() {
        return expression;
    }

    public ASTNode getIfScope() {
        return ifScope;
    }

    public ASTNode getElseScope() {
        return elseScope;
    }
}
