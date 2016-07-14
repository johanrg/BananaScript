package com.github.johanrg.ast;

import java.util.List;
import java.util.Map;

/**
 * @author johan
 * @since 2016-07-02.
 */
public class ASTScope extends ASTNode {
    private final List<ASTNode> statements;
    private final Map<String, Identifier> identifiers;

    public ASTScope(List<ASTNode> statements, Map<String, Identifier> identifiers) {
        super(null);
        this.statements = statements;
        this.identifiers = identifiers;
    }

    public List<ASTNode> getStatements() {
        return statements;
    }

    public Map<String, Identifier> getIdentifiers() {
        return identifiers;
    }
}
