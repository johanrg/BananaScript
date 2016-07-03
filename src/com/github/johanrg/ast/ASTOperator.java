package com.github.johanrg.ast;

import com.github.johanrg.compiler.Location;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTOperator implements ASTNode {
    public enum Associativity {
        LEFT,
        RIGHT;
    }
    public enum Group {
        ASSIGNMENT,
        BINARY,
        UNARY,
        RELATIONAL,
        DELIMITER;
    }
    public enum Type {
        ASSIGNMENT("=", Group.ASSIGNMENT, 1, Associativity.RIGHT),
        ADD_ASSIGNMENT("+=", Group.ASSIGNMENT, 1, Associativity.RIGHT),
        SUB_ASSIGNMENT("-=", Group.ASSIGNMENT, 1, Associativity.RIGHT),
        MUL_ASSIGNMENT("*=", Group.ASSIGNMENT, 1, Associativity.RIGHT),
        DIV_ASSIGNMENT("/=", Group.ASSIGNMENT, 1, Associativity.RIGHT),

        LOGICAL_OR("||", Group.BINARY, 3, Associativity.LEFT),
        LOGICAL_AND("&&", Group.BINARY, 4, Associativity.LEFT),

        RELATIONAL_EQUAL("==", Group.RELATIONAL, 8, Associativity.LEFT),
        RELATIONAL_NOT_EQUAL("!=", Group.RELATIONAL, 8, Associativity.LEFT),

        BINARY_ADD("+", Group.BINARY, 11, Associativity.LEFT),
        BINARY_SUB("-", Group.BINARY, 11, Associativity.LEFT),

        BINARY_DIV("/", Group.BINARY, 12, Associativity.LEFT),
        BINARY_MOD("%", Group.BINARY, 12, Associativity.LEFT),
        BINARY_MUL("*", Group.BINARY, 12, Associativity.LEFT),

        UNARY_PLUS("+", Group.UNARY, 13, Associativity.RIGHT),
        UNARY_MINUS("-", Group.UNARY, 13, Associativity.RIGHT),
        UNARY_LOGICAL_NEGATION("!", Group.UNARY, 13, Associativity.RIGHT),
        UNARY_PRE_INCREMENT("++", Group.UNARY, 13, Associativity.RIGHT),
        UNARY_PRE_DECREMENT("--", Group.UNARY, 13, Associativity.RIGHT),

        BINARY_POWER("^", Group.BINARY, 13, Associativity.RIGHT),

        UNARY_POST_INCREMENT("++", Group.UNARY, 14, Associativity.RIGHT),
        UNARY_POST_DECREMENT("--", Group.UNARY, 14, Associativity.RIGHT),

        OPEN_PARENTHESES("(", Group.DELIMITER, 15, Associativity.LEFT),
        CLOSE_PARENTHESES(")", Group.DELIMITER, 15, Associativity.LEFT);

        private final String symbol;
        private final Group group;
        private final int precedence;
        private final Associativity associativity;

        Type(String symbol, Group group, int precedence, Associativity associativity) {
            this.symbol = symbol;
            this.group = group;
            this.precedence = precedence;
            this.associativity = associativity;
        }

        public Group getGroup() {
            return group;
        }

        public int getPrecedence() {
            return precedence;
        }

        public Associativity getAssociativity() {
            return associativity;
        }
    }

    private final Type type;

    public ASTOperator(Type type) {
        this.type = type;
    }

    public static Type match(String symbol) {
        for(ASTOperator.Type type : ASTOperator.Type.values()) {
            if (type.symbol.equals(symbol)) {
                return type;
            }
        }
        return null;
    }

    public Type getType() {
        return type;
    }

}
