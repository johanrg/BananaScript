package com.github.johanrg.compiler;

import com.github.johanrg.ast.ASTNode;
import java.util.Queue;

/**
 * @author johan
 * @since 2016-07-02.
 */
public class Parser {
    private ASTNode globalNode = null;
    private final Queue<Token> tokens;

    public Parser(Queue<Token> tokens) {
        this.tokens = tokens;
    }

    private void parse() {

    }
}
