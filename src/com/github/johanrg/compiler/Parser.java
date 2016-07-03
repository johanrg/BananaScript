package com.github.johanrg.compiler;

import com.github.johanrg.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.github.johanrg.compiler.DataType.INT;

/**
 * @author johan
 * @since 2016-07-02.
 */
public class Parser {
    private ASTNode globalNode = null;
    private final List<Token> tokens;
    private final Stack<ASTOperator.Type> operatorStack = new Stack<>();
    private final Stack<ASTNode> expressionStack = new Stack<>();
    private int start = 0;
    private int pos = 0;

    public Parser(List<Token> tokens) throws CompilerException {
        this.tokens = tokens;
        parse();
    }

    private void parse() throws CompilerException {
        //System.out.println(parseIdentifierDeclarationStatement());
        accept("(");
        ignore();
        ASTNode first = parseExpression();
        accept(",");
        ignore();
        ASTNode second = parseExpression();
    }

    private ASTNode parseIdentifierDeclarationStatement() throws CompilerException {
        DataType dataType;
        expect(Token.Type.IDENTIFIER);
        String identifier = save().getData();
        expect(":");
        ignore();
        if (accept(Token.Type.IDENTIFIER)) {
            dataType = ASTLiteral.typeForName(save().getData());
            if (dataType == null) {
                error("expected valid data type");
            }
        } else {
            dataType = DataType.AUTO;
        }
        if (accept(":")) {
            ignore();
            if (checkIfFunction()) {
                // read in parameter list
                expect("(");
                ignore();
                List<ASTNode> parameters = new ArrayList<>();
                while (!check(")") & !check(Token.Type.END_OF_STATEMENT, Token.Type.EOF)) {
                    parameters.add(parseIdentifierDeclarationStatement());
                    while (accept(",")) {
                        ignore();
                        parameters.add(parseIdentifierDeclarationStatement());
                    }
                }
                expect(")");
                DataType returnType = null;
                if (accept("->")) {
                    ignore();
                    expect(Token.Type.IDENTIFIER);
                    returnType = ASTLiteral.typeForName(save().getData());
                    if (returnType == null) {
                        error("expected valid data type");
                    }
                }
                expect(Token.Type.END_OF_STATEMENT);
                ignore();
                System.out.println("valid function header");
                System.out.println(identifier);
                System.out.println(parameters);
                System.out.println(returnType);
            } else {
                // variable
                System.out.println("read in expression for variable");
                System.out.println("create variable with expression");
            }
        } else if (dataType != DataType.AUTO) {
            return new ASTVariable(identifier, ASTLiteral.defaultValueForType(dataType), dataType);
        } else {
            error("data type auto with no expression");
        }
        return null;
    }

    private ASTNode parseExpression() throws CompilerException {
        boolean operator = false;
        int parentheses = 0;
        while (!check(Token.Type.EOF, Token.Type.END_OF_STATEMENT) &&
                !(parentheses == 0 && check(")")) && !check(",") ) {
            if (accept("(")) {
                if (!operator && !expressionStack.isEmpty()) {
                    backup();
                    error("did not expect '('");
                }
                ++parentheses;
                operator = true;
                ignore();
                operatorStack.push(ASTOperator.Type.OPEN_PARENTHESES);
            }

            if (accept(Token.Type.LITERAL)) {
                if (!operator && !expressionStack.isEmpty()) {
                    backup();
                    error("did not expect literal");
                }
                operator = false;
                Token t = save();
                expressionStack.push(new ASTLiteral(makeLiteralType(t), t.getDataType()));
            }

            if (accept(")")) {
                --parentheses;
                if (parentheses < 0) {
                    backup();
                    break;
                }
                while (operatorStack.peek() != ASTOperator.Type.OPEN_PARENTHESES) {
                    popOperatorOntoExpressionStack();
                }
                operatorStack.pop();
            }

            if (accept(Token.Type.OPERATOR)) {
                ASTOperator.Type op = ASTOperator.match(save().getData());
                if (op == null) {
                    backup();
                    error("not a valid operator");
                }
                if (op.getGroup() != ASTOperator.Group.UNARY && (expressionStack.isEmpty() && operator)) {
                    if (op == ASTOperator.Type.BINARY_ADD) {
                        op = ASTOperator.Type.UNARY_PLUS;
                    } else if (op == ASTOperator.Type.BINARY_SUB) {
                        op = ASTOperator.Type.BINARY_SUB;
                    } else {
                        error("did not expect binary operator");
                    }
                }
                operator = true;
                if (op.getAssociativity() == ASTOperator.Associativity.LEFT) {
                    while (!operatorStack.isEmpty() && operatorStack.peek().getPrecedence() >= op.getPrecedence()) {
                        popOperatorOntoExpressionStack();
                    }
                } else {
                    while (!operatorStack.isEmpty() && operatorStack.peek().getPrecedence() > op.getPrecedence()) {
                        popOperatorOntoExpressionStack();
                    }
                }

                operatorStack.push(op);
            }
        }
        if (parentheses > 0) {
            error("expected ')'");
        }
        while (!operatorStack.isEmpty()) {
            popOperatorOntoExpressionStack();
        }
        if (expressionStack.isEmpty()) {
            error("expected expression");
        }
        return expressionStack.pop();
    }

    private void popOperatorOntoExpressionStack() throws CompilerException {
        ASTOperator.Type op = operatorStack.pop();
        if (op.getGroup() == ASTOperator.Group.UNARY) {
            if (expressionStack.isEmpty()) {
                error("expected operand");
            }
            ASTNode node = expressionStack.pop();
            expressionStack.push(new ASTUnaryOperator(op, node));
        } else if (op.getGroup() == ASTOperator.Group.BINARY) {
            if (expressionStack.size() < 2) {
                error("expected operand");
            }

            // NOTE(Johan) Careful, observe order
            ASTNode rightNode = expressionStack.pop();
            ASTNode leftNode = expressionStack.pop();

            expressionStack.push(new ASTBinaryOperator(op, leftNode, rightNode));
        }
    }

    private Object makeLiteralType(Token token) throws CompilerException {
        try {
            switch (token.getDataType()) {
                case INT:
                    return Integer.parseInt(token.getData());
                case FLOAT:
                    return Float.parseFloat(token.getData());
                case DOUBLE:
                    return Double.parseDouble(token.getData());
                case CHAR:
                    return token.getData().charAt(0);
                case STRING:
                    return token.getData();
            }
        } catch (NumberFormatException e) {
            error(String.format("literal is not a valid %s", token.getDataType().toString()));
        }
        assert false : "makeLiteralType could not find the type";
        return null;
    }

    private boolean checkIfFunction() {
        boolean isFunction = accept("(") && accept(Token.Type.IDENTIFIER) && accept(":");
        revert();
        if (!isFunction) {
            isFunction = accept("(") && accept(")");
            revert();
        }
        return isFunction;
    }

    private void error(String error) throws CompilerException {
        throw new CompilerException(String.format("Error:(%d,%d) %s (%s)", peek().getLocation().getLine(), peek().getLocation().getColumn(), error, peek().getLocation().getFileName()));
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token next() {
        return tokens.get(pos++);
    }

    private void backup() {
        --pos;
    }

    private void revert() {
        pos = start;
    }

    private void ignore() {
        start = pos;
    }

    private Token save() {
        return tokens.get(start++);
    }

    private boolean accept(Token.Type... valid) {
        Token t = next();
        for (Token.Type v : valid) {
            if (t.getType() == v) {
                return true;
            }
        }
        backup();
        return false;
    }

    private boolean accept(String... valid) {
        String t = next().getData();
        for (String v : valid) {
            if (t.equals(v)) {
                return true;
            }
        }
        backup();
        return false;
    }

    private boolean check(Token.Type... valid) {
        Token t = peek();
        for (Token.Type v : valid) {
            if (t.getType() == v) {
                return true;
            }
        }
        return false;
    }

    private boolean check(String... valid) {
        String t = peek().getData();
        for (String v : valid) {
            if (t.equals(v)) {
                return true;
            }
        }
        return false;
    }

    private boolean expect(Token.Type valid) throws CompilerException {
        Token t = next();
        if (t.getType() == valid) {
            return true;
        }
        backup();
        error(String.format("expected '%s'", valid.toString().toLowerCase()));
        return false;
    }

    private boolean expect(String expected) throws CompilerException {
        String t = next().getData();
        if (t.equals(expected)) {
            return true;
        }
        backup();
        error(String.format("expected '%s'", expected));
        return false;
    }
}
