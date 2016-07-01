package com.github.johanrg.ast;

import com.github.johanrg.compiler.Location;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTVariable<T> extends ASTLiteral<T> implements Identifier {
    private final String identifier;

    public ASTVariable(String identifier, T value, Location location) {
        super(value, location);
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }
}
