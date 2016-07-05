package com.github.johanrg.compiler;

import com.github.johanrg.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author johan
 * @since 2016-07-02.
 */
public class Parser {
    private ASTNode globalNode = null;
    private final List<Token> tokens;
    private final Stack<ASTOperator> operatorStack = new Stack<>();
    private final Stack<ASTNode> expressionStack = new Stack<>();
    private int start = 0;
    private int pos = 0;

    public Parser(List<Token> tokens) throws CompilerException {
        this.tokens = tokens;
        parse();
    }

    private void parse() throws CompilerException {
        ASTNode node = parseIdentifierDeclarationStatement();
        //ASTNode first = parseExpression();
    }

    private ASTNode parseIdentifierDeclarationStatement() throws CompilerException {
        DataType identifierDataType;
        expect(Token.Type.IDENTIFIER);
        Token identifierToken = save();
        // check data type
        expect(":");
        ignore();
        if (accept(Token.Type.IDENTIFIER)) {
            identifierDataType = ASTLiteral.typeForName(save().getData());
            if (identifierDataType == null) {
                error("expected valid data type");
            }
        } else {
            identifierDataType = DataType.AUTO;
        }
        // constant
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
                    } else if (returnType == DataType.AUTO) {
                        backup();
                        error("return data type for function can not be auto");
                    }
                }
                expect(Token.Type.END_OF_STATEMENT);
                ignore();
                System.out.println("valid function header");
                System.out.println(identifierToken);
                System.out.println(parameters);
                System.out.println(returnType);
            } else {
                // Constant identifier
                ASTNode node = parseExpression();
                DataType expressionDataType = typeCheckExpression(node);
                if (identifierDataType == DataType.AUTO) {
                    identifierDataType = expressionDataType;
                } else if (identifierDataType != expressionDataType) {
                    error(String.format("expected expression of type: '%s", identifierDataType.toString().toLowerCase()), node.getLocation());
                }
                return new ASTBinaryOperator(ASTOperator.Type.ASSIGNMENT,
                        new ASTVariable(identifierToken.getData(), null, identifierDataType, true, identifierToken.getLocation()),
                        node, identifierToken.getLocation());
            }
            //variable
        } else if (accept("=")) {
            ignore();
            ASTNode node = parseExpression();
            DataType expressionDataType = typeCheckExpression(node);
            if (identifierDataType == DataType.AUTO) {
                identifierDataType = expressionDataType;
            } else if (identifierDataType != expressionDataType) {
                error(String.format("expected expression of type: '%s", identifierDataType.toString().toLowerCase()), node.getLocation());
            }
            ASTLiteral result = simplifyExpressionIfPossible(node);
            if (result == null) {
                return new ASTBinaryOperator(ASTOperator.Type.ASSIGNMENT,
                        new ASTVariable(identifierToken.getData(), null, identifierDataType, false, identifierToken.getLocation()),
                        node, identifierToken.getLocation());
            } else {
                return new ASTVariable(identifierToken.getData(), result.getValue(), identifierDataType, false, identifierToken.getLocation());
            }
        } else {
            error("data type auto with no expression");
        }
        return null;
    }

    private ASTNode parseExpression() throws CompilerException {
        boolean operator = false;
        int parentheses = 0;
        while (!check(Token.Type.EOF, Token.Type.END_OF_STATEMENT) &&
                !(parentheses == 0 && check(")")) && !check(",")) {
            if (accept("(")) {
                if (!operator && !expressionStack.isEmpty()) {
                    backup();
                    error("did not expect '('");
                }
                ++parentheses;
                operator = true;
                Token t = save();
                ASTOperator astOperator = new ASTOperator(ASTOperator.Type.OPEN_PARENTHESES, t.getLocation());
                operatorStack.push(astOperator);
            }

            if (accept(Token.Type.LITERAL)) {
                if (!operator && !expressionStack.isEmpty()) {
                    backup();
                    error("did not expect literal");
                }
                operator = false;
                Token t = save();
                expressionStack.push(new ASTLiteral(makeLiteralType(t), t.getDataType(), t.getLocation()));
            }

            if (accept(")")) {
                ignore();
                --parentheses;
                if (parentheses < 0) {
                    backup();
                    break;
                }
                while (!operatorStack.isEmpty() && operatorStack.peek().getType() != ASTOperator.Type.OPEN_PARENTHESES) {
                    popOperatorOntoExpressionStack();
                }
                operatorStack.pop();
            }

            if (accept(Token.Type.OPERATOR)) {
                Token operatorToken = save();
                ASTOperator.Type op = ASTOperator.match(operatorToken.getData());
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
                        error("did not expect operator");
                    }
                }
                operator = true;
                if (op.getAssociativity() == ASTOperator.Associativity.LEFT) {
                    while (!operatorStack.isEmpty() && operatorStack.peek().getType().getPrecedence() >= op.getPrecedence()) {
                        popOperatorOntoExpressionStack();
                    }
                } else {
                    while (!operatorStack.isEmpty() && operatorStack.peek().getType().getPrecedence() > op.getPrecedence()) {
                        popOperatorOntoExpressionStack();
                    }
                }

                operatorStack.push(new ASTOperator(op, operatorToken.getLocation()));
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

    private DataType typeCheckExpression(ASTNode node) throws CompilerException {
        if (node instanceof ASTVariable) {
            return ((ASTVariable) node).getDataType();
        } else if (node instanceof ASTLiteral) {
            return ((ASTLiteral) node).getDataType();
        } else if (node instanceof ASTFunction) {
            return ((ASTFunction) node).getReturnDataType();
        } else if (node instanceof ASTUnaryOperator) {
            return typeCheckExpression(((ASTUnaryOperator) node).getSingleNode());
        } else if (node instanceof ASTBinaryOperator) {
            DataType left = typeCheckExpression(((ASTBinaryOperator) node).getLeft());
            DataType right = typeCheckExpression(((ASTBinaryOperator) node).getRight());
            if (left.equals(right)) {
                return left;
            } else {
                error("type mismatch", node.getLocation());
            }
        }
        assert false : "Node not supported";
        return null;
    }

    private ASTLiteral simplifyExpressionIfPossible(ASTNode node) throws CompilerException {
        if (node instanceof ASTLiteral) {
            return (ASTLiteral) node;
        } else if (node instanceof ASTBinaryOperator) {
            ASTLiteral left = simplifyExpressionIfPossible(((ASTBinaryOperator) node).getLeft());
            ASTLiteral right = simplifyExpressionIfPossible(((ASTBinaryOperator) node).getRight());
            if (left == null || right == null) {
                return null;
            }

            switch (((ASTBinaryOperator) node).getType()) {
                case BINARY_ADD:
                    return solveAdd(left, right);
                case BINARY_SUB:
                    return solveSub(left, right);
                case BINARY_MUL:
                    return solveMul(left, right);
                case BINARY_DIV:
                    return solveDiv(left, right);
                case BINARY_MOD:
                    return solveMod(left, right);
                case BINARY_POW:
                    return solvePow(left, right);
            }
        }
        return null;
    }

    private ASTLiteral solveAdd(ASTLiteral left, ASTLiteral right) throws CompilerException {
        switch (left.getDataType()) {
            case BOOLEAN:
                error("binary addition not allowed with boolean type", left.getLocation());
                break;
            case INT:
                return new ASTLiteral(left.getInt() + right.getInt(), DataType.INT, left.getLocation());
            case FLOAT:
                return new ASTLiteral(left.getFloat() + right.getFloat(), DataType.FLOAT, left.getLocation());
            case DOUBLE:
                return new ASTLiteral(left.getDouble() + right.getDouble(), DataType.DOUBLE, left.getLocation());
            case CHAR:
                error("binary additon not allwoed with char type");
                break;
            case STRING:
                return new ASTLiteral(left.getString() + right.getString(), DataType.STRING, left.getLocation());
            default:
                assert false : "binary add does not support this type.";
        }
        return null;
    }

    private ASTLiteral solveSub(ASTLiteral left, ASTLiteral right) throws CompilerException {
        switch (left.getDataType()) {
            case BOOLEAN:
                error("binary subtraction not allowed with boolean type", left.getLocation());
                break;
            case INT:
                return new ASTLiteral(left.getInt() - right.getInt(), DataType.INT, left.getLocation());
            case FLOAT:
                return new ASTLiteral(left.getFloat() - right.getFloat(), DataType.FLOAT, left.getLocation());
            case DOUBLE:
                return new ASTLiteral(left.getDouble() - right.getDouble(), DataType.DOUBLE, left.getLocation());
            case CHAR:
                error("binary subtractions not allowed with char");
                break;
            case STRING:
                error("binary subtractions not allowed with string", left.getLocation());
                break;
            default:
                assert false : "binary subtraction does not support this type";
        }
        return null;
    }


    private ASTLiteral solveMul(ASTLiteral left, ASTLiteral right) throws CompilerException {
        switch (left.getDataType()) {
            case BOOLEAN:
                error("binary multiplication not allowed with boolean type", left.getLocation());
                break;
            case INT:
                return new ASTLiteral(left.getInt() * right.getInt(), DataType.INT, left.getLocation());
            case FLOAT:
                return new ASTLiteral(left.getFloat() * right.getFloat(), DataType.FLOAT, left.getLocation());
            case DOUBLE:
                return new ASTLiteral(left.getDouble() * right.getDouble(), DataType.DOUBLE, left.getLocation());
            case CHAR:
                error("binary multiplication not allowed with char");
                break;
            case STRING:
                error("binary multiplication not allowed with string", left.getLocation());
                break;
            default:
                assert false : "binary multiplication does not support this type";
        }
        return null;
    }

    private ASTLiteral solveDiv(ASTLiteral left, ASTLiteral right) throws CompilerException {
        switch (left.getDataType()) {
            case BOOLEAN:
                error("binary division not allowed with boolean type", left.getLocation());
                break;
            case INT:
                return new ASTLiteral(left.getInt() / right.getInt(), DataType.INT, left.getLocation());
            case FLOAT:
                return new ASTLiteral(left.getFloat() / right.getFloat(), DataType.FLOAT, left.getLocation());
            case DOUBLE:
                return new ASTLiteral(left.getDouble() / right.getDouble(), DataType.DOUBLE, left.getLocation());
            case CHAR:
                error("binary division not allowed with char");
                break;
            case STRING:
                error("binary division not allowed with string", left.getLocation());
                break;
            default:
                assert false : "binary division does not support this type";
        }
        return null;
    }

    private ASTLiteral solveMod(ASTLiteral left, ASTLiteral right) throws CompilerException {
        switch (left.getDataType()) {
            case BOOLEAN:
                error("binary modulus not allowed with boolean type", left.getLocation());
                break;
            case INT:
                return new ASTLiteral(left.getInt() % right.getInt(), DataType.INT, left.getLocation());
            case FLOAT:
                return new ASTLiteral(left.getFloat() % right.getFloat(), DataType.FLOAT, left.getLocation());
            case DOUBLE:
                return new ASTLiteral(left.getDouble() % right.getDouble(), DataType.DOUBLE, left.getLocation());
            case CHAR:
                error("binary modulus not allowed with char");
                break;
            case STRING:
                error("binary modulus not allowed with string", left.getLocation());
                break;
            default:
                assert false : "binary modulus does not support this type";
        }
        return null;
    }

    private ASTLiteral solvePow(ASTLiteral left, ASTLiteral right) throws CompilerException {
        switch (left.getDataType()) {
            case BOOLEAN:
                error("binary exponent not allowed with boolean type", left.getLocation());
                break;
            case INT:
                return new ASTLiteral((int) Math.pow((double) left.getInt(), (double) right.getInt()), DataType.INT, left.getLocation());
            case FLOAT:
                return new ASTLiteral((float) Math.pow(left.getFloat(), right.getFloat()), DataType.FLOAT, left.getLocation());
            case DOUBLE:
                return new ASTLiteral(Math.pow(left.getDouble(), right.getDouble()), DataType.DOUBLE, left.getLocation());
            case CHAR:
                error("binary exponent not allowed with char");
                break;
            case STRING:
                error("binary exponent not allowed with string", left.getLocation());
                break;
            default:
                assert false : "binary exponent does not support this type";
        }
        return null;
    }

    private void popOperatorOntoExpressionStack() throws CompilerException {
        ASTOperator astOperator = operatorStack.pop();
        if (astOperator.getType().getGroup() == ASTOperator.Group.UNARY) {
            if (expressionStack.isEmpty()) {
                error("expected operand");
            }
            ASTNode node = expressionStack.pop();
            expressionStack.push(new ASTUnaryOperator(astOperator.getType(), node, astOperator.getLocation()));
        } else if (astOperator.getType().getGroup() == ASTOperator.Group.BINARY ||
                astOperator.getType().getGroup() == ASTOperator.Group.ASSIGNMENT) {
            if (expressionStack.size() < 2) {
                error("expected operand");
            }

            // NOTE(Johan) Careful, observe order
            ASTNode rightNode = expressionStack.pop();
            ASTNode leftNode = expressionStack.pop();

            expressionStack.push(new ASTBinaryOperator(astOperator.getType(), leftNode, rightNode, astOperator.getLocation()));
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

    private void error(String error, Location location) throws CompilerException {
        throw new CompilerException(String.format("Error:(%d,%d) %s (%s)", location.getLine(), location.getColumn(), error, location.getFileName()));
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
