package com.github.johanrg.ast1;

import com.github.johanrg.TokenType;

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
