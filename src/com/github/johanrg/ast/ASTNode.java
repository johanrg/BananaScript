package com.github.johanrg.ast;

import com.github.johanrg.compiler.Location;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTNode {
    private final Location location;

    public ASTNode(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}
