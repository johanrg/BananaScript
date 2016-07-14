package com.github.johanrg.backend;

import com.github.johanrg.ast.ASTNode;

/**
 * @author Johan Gustafsson
 * @since 7/14/2016.
 */
public class Compile {
    private final ASTNode root;

    Compile(ASTNode root) {
        this.root = root;
    }
}
