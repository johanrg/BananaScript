package com.github.johanrg.ast;

import com.github.johanrg.compiler.Location;

/**
 * @author johan
 * @since 2016-06-30.
 */
public class ASTOperator extends ASTNode {
    public enum Associativity {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT;
    }
    public enum Group {
        ASSIGNMENT,
        BINARY,
        UNARY,
        RELATIONAL,
        DELIMITER;
    }
    public enum Type {
        ASSIGNMENT("=", Group.ASSIGNMENT, 1, Associativity.RIGHT_TO_LEFT),
        ADD_ASSIGNMENT("+=", Group.ASSIGNMENT, 1, Associativity.RIGHT_TO_LEFT),
        SUB_ASSIGNMENT("-=", Group.ASSIGNMENT, 1, Associativity.RIGHT_TO_LEFT),
        MUL_ASSIGNMENT("*=", Group.ASSIGNMENT, 1, Associativity.RIGHT_TO_LEFT),
        DIV_ASSIGNMENT("/=", Group.ASSIGNMENT, 1, Associativity.RIGHT_TO_LEFT),

        LOGICAL_OR("||", Group.BINARY, 3, Associativity.LEFT_TO_RIGHT),
        LOGICAL_AND("&&", Group.BINARY, 4, Associativity.LEFT_TO_RIGHT),

        RELATIONAL_EQUAL("==", Group.RELATIONAL, 8, Associativity.LEFT_TO_RIGHT),
        RELATIONAL_NOT_EQUAL("!=", Group.RELATIONAL, 8, Associativity.LEFT_TO_RIGHT),

        RELATIONAL_LT("<", Group.RELATIONAL, 9, Associativity.LEFT_TO_RIGHT),
        RELATIONAL_LTE("<=", Group.RELATIONAL, 9, Associativity.LEFT_TO_RIGHT),
        RELATIONAL_GT(">", Group.RELATIONAL, 9, Associativity.LEFT_TO_RIGHT),
        RELATIONAL_GTE(">=", Group.RELATIONAL, 9, Associativity.LEFT_TO_RIGHT),

        BINARY_ADD("+", Group.BINARY, 11, Associativity.LEFT_TO_RIGHT),
        BINARY_SUB("-", Group.BINARY, 11, Associativity.LEFT_TO_RIGHT),

        BINARY_DIV("/", Group.BINARY, 12, Associativity.LEFT_TO_RIGHT),
        BINARY_MOD("%", Group.BINARY, 12, Associativity.LEFT_TO_RIGHT),
        BINARY_MUL("*", Group.BINARY, 12, Associativity.LEFT_TO_RIGHT),

        UNARY_PLUS("+", Group.UNARY, 13, Associativity.RIGHT_TO_LEFT),
        UNARY_MINUS("-", Group.UNARY, 13, Associativity.RIGHT_TO_LEFT),
        UNARY_LOGICAL_NEGATION("!", Group.UNARY, 13, Associativity.RIGHT_TO_LEFT),
        UNARY_PRE_INCREMENT("++", Group.UNARY, 13, Associativity.RIGHT_TO_LEFT),
        UNARY_PRE_DECREMENT("--", Group.UNARY, 13, Associativity.RIGHT_TO_LEFT),

        BINARY_POW("^", Group.BINARY, 13, Associativity.RIGHT_TO_LEFT),

        UNARY_POST_INCREMENT("++", Group.UNARY, 14, Associativity.RIGHT_TO_LEFT),
        UNARY_POST_DECREMENT("--", Group.UNARY, 14, Associativity.RIGHT_TO_LEFT),

        OPEN_PARENTHESES("(", Group.DELIMITER, 0, Associativity.LEFT_TO_RIGHT),
        CLOSE_PARENTHESES(")", Group.DELIMITER, 0, Associativity.LEFT_TO_RIGHT);

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

    public ASTOperator(Type type, Location location) {
        super(location);
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
