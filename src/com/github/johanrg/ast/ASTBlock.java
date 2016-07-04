package com.github.johanrg.ast;

import com.github.johanrg.compiler.Location;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author johan
 * @since 2016-07-02.
 */
public class ASTBlock extends ASTNode {
    private final List<ASTNode> statements;
    private final Map<String, Identifier> identifiers;

    public ASTBlock(List<ASTNode> statements, Map<String, Identifier> identifiers, Location location) {
        super(location);
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
