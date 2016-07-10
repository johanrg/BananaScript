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
    private final List<Token> tokens;
    private final Identifiers identifiers = new Identifiers();
    private final Stack<ASTOperator> operatorStack = new Stack<>();
    private final Stack<ASTNode> expressionStack = new Stack<>();
    private int start = 0;
    private int pos = 0;
    private int currentScopeLevel = 0;

    public Parser(List<Token> tokens) throws CompilerException {
        this.tokens = tokens;
        parseScope();
    }

    private ASTNode parseStatement() throws CompilerException {
        ASTNode node = null;
        if (checkIfDeclaration()) {
            node = parseIdentifierDefinitionStatement();
        } else if (checkIfAssignment()) {
            node = parseVariableAssignment();
        }
        return node;
    }

    private ASTCompoundStatement parseScope() throws CompilerException {
        List<ASTNode> statements = new ArrayList<>();
        identifiers.newScope();
        while (!eof() && scopeDiff(0)) {
            statements.add(parseStatement());
            if (scopeDiff(1)) {
                ++currentScopeLevel;
                statements.add(parseScope());
            }
        }
        --currentScopeLevel;
        return new ASTCompoundStatement(statements, identifiers.popScope());
    }

    private ASTNode parseIdentifierDefinitionStatement() throws CompilerException {
        DataType identifierDataType;
        expect(Token.Type.IDENTIFIER);
        Token identifierToken = save();

        // check if declaration
        expect(":");
        Token dataTypeToken = null;
        if (accept(Token.Type.IDENTIFIER)) {
            dataTypeToken = save();
            identifierDataType = ASTLiteral.typeForName(dataTypeToken.getData());
            if (identifierDataType == null) {
                error("invalid data type");
            }
        } else {
            identifierDataType = DataType.AUTO;
        }
        // constant
        if (accept(":")) {
            if (checkIfFunction()) {
                // read in parameter list
                expect("(");
                List<ASTNode> parameters = new ArrayList<>();
                while (!check(")") & !check(Token.Type.END_OF_STATEMENT, Token.Type.EOF)) {
                    parameters.add(parseIdentifierDefinitionStatement());
                    while (accept(",")) {
                        parameters.add(parseIdentifierDefinitionStatement());
                    }
                }
                expect(")");
                DataType returnType = DataType.VOID;
                if (accept("->")) {
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
                if (scopeDiff(1)) {
                    ++currentScopeLevel;
                    ASTCompoundStatement compoundStatement = parseScope();
                    ASTFunction function = new ASTFunction(identifierToken.getData(), parameters, compoundStatement,
                            returnType, identifierToken.getLocation());
                    if (!identifiers.addIdentifier(function)) {
                        error(String.format("'%s' is already defined in this scope.", identifierToken.getData()),
                                identifierToken.getLocation());
                    }
                    return function;
                } else {
                    System.out.println("invalid scope");
                }
            } else {
                // Constant identifier
                ASTNode node = parseExpression();
                expect(Token.Type.END_OF_STATEMENT);
                return assignExpressionInVariableDefinition(ASTOperator.Type.ASSIGNMENT, identifierToken, dataTypeToken,
                        identifierDataType, node, true);
            }
            //variable
        } else if (accept("=")) {
            ASTNode node = parseExpression();
            expect(Token.Type.END_OF_STATEMENT);
            return assignExpressionInVariableDefinition(ASTOperator.Type.ASSIGNMENT, identifierToken, dataTypeToken,
                    identifierDataType, node, false);
        } else {
            error("data type auto with no expression");
        }
        return null;
    }

    private ASTNode parseVariableAssignment() throws CompilerException {
        expect(Token.Type.IDENTIFIER);
        Token identifierToken = save();
        expect(Token.Type.OPERATOR);
        Token operator = save();
        ASTOperator.Type assignmentType = ASTOperator.match(operator.getData());
        if (assignmentType != null && assignmentType.getGroup() != ASTOperator.Group.ASSIGNMENT) {
            error("expected assignment");
        }
        ASTNode node = parseExpression();
        expect(Token.Type.END_OF_STATEMENT);
        Identifier identifier = identifiers.find(identifierToken.getData());
        if (identifier != null) {
            DataType identifierDataType = Expression.typeCheck((ASTNode) identifier);
            DataType expressionDataType = Expression.typeCheck(node);
            if (identifierDataType == expressionDataType) {
                return new ASTBinaryOperator(assignmentType,
                        (ASTNode) identifier,
                        node, identifierToken.getLocation());

            } else {
                error("type mismatch", node.getLocation());
            }
        } else {
            error(String.format("can not resolve symbol '%s'", identifierToken.getData()), identifierToken.getLocation());
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
                Token token = save();
                expressionStack.push(new ASTLiteral(makeLiteralType(token), token.getDataType(), token.getLocation()));
            }

            if (accept(Token.Type.IDENTIFIER)) {
                Token token = save();
                Identifier identifier = identifiers.find((token.getData()));
                if (identifier != null) {
                    if (identifier instanceof ASTVariable) {
                        expressionStack.push((ASTVariable) identifier);
                    } else if (identifier instanceof ASTFunction) {
                        expressionStack.push((ASTFunction) identifier);
                    } else {
                        error(String.format("Not a valid type '%s'", token.getData()), token.getLocation());
                    }
                }
            }

            if (accept(")")) {
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
                    while (!operatorStack.isEmpty() &&
                            operatorStack.peek().getType().getPrecedence() >= op.getPrecedence()) {
                        popOperatorOntoExpressionStack();
                    }
                } else {
                    while (!operatorStack.isEmpty() &&
                            operatorStack.peek().getType().getPrecedence() > op.getPrecedence()) {
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

    private ASTNode assignExpressionInVariableDefinition(ASTOperator.Type assignmentType, Token identifierToken,
                                                         Token dataTypeToken, DataType identifierDataType, ASTNode node,
                                                         boolean constant) throws CompilerException {
        ASTNode result = null;
        identifierDataType = Expression.typeCheckVsDataType(node, dataTypeToken, identifierDataType);
        ASTLiteral expression = Expression.simplifyExpressionIfPossible(node);
        if (expression != null) {
            node = expression;
        }
        ASTVariable variable = new ASTVariable(identifierToken.getData(), null, identifierDataType, constant,
                identifierToken.getLocation());
        if (identifiers.addIdentifier(variable)) {
            result = new ASTBinaryOperator(assignmentType,
                    variable,
                    node, identifierToken.getLocation());
        } else {
            error(String.format("'%s' is already defined in this scope.", identifierToken.getData()),
                    identifierToken.getLocation());
        }
        return result;
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

            expressionStack.push(new ASTBinaryOperator(astOperator.getType(), leftNode, rightNode,
                    astOperator.getLocation()));
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

    private boolean checkIfDeclaration() throws CompilerException {
        bookmark();
        boolean isDeclaration = accept(Token.Type.IDENTIFIER) && accept(":");
        revert();
        return isDeclaration;
    }

    private boolean checkIfAssignment() throws CompilerException {
        bookmark();
        boolean isAssignment = false;
        if (accept(Token.Type.IDENTIFIER)) {
            ASTOperator.Type assignment = ASTOperator.match(next().getData());
            if (assignment != null) {
                isAssignment = assignment.getGroup() == ASTOperator.Group.ASSIGNMENT;
            }

        }
        revert();
        return isAssignment;
    }

    private boolean checkIfFunction() throws CompilerException {
        bookmark();
        boolean isFunction = accept("(") && accept(Token.Type.IDENTIFIER) && accept(":");
        revert();
        if (!isFunction) {
            isFunction = accept("(") && accept(")");
            revert();
        }
        return isFunction;
    }

    private void error(String error) throws CompilerException {
        throw new CompilerException(String.format("Error:(%d,%d) %s (%s)", peek().getLocation().getLine(),
                peek().getLocation().getColumn(), error, peek().getLocation().getFileName()));
    }

    private void error(String error, Location location) throws CompilerException {
        throw new CompilerException(String.format("Error:(%d,%d) %s (%s)", location.getLine(), location.getColumn(),
                error, location.getFileName()));
    }

    private void checkScope(Token token) throws CompilerException {
        int diff = currentScopeLevel - token.getScopeLevel();
        if (diff > 1 || diff < -1) {
            error("illegal scope change", token.getLocation());
        }
    }

    private boolean scopeDiff(int level) throws CompilerException {
        int diff = peek().getScopeLevel() - currentScopeLevel;
        return diff == level;
    }

    private boolean eof() throws CompilerException {
        return check(Token.Type.EOF);
    }

    private Token peek() throws CompilerException {
        Token token = tokens.get(pos);
        checkScope(token);
        return token;
    }

    private Token next() throws CompilerException {
        Token token = tokens.get(pos++);
        checkScope(token);
        currentScopeLevel = token.getScopeLevel();
        return token;
    }

    private void backup() {
        --pos;
    }

    private void bookmark() {
        start = pos;
    }

    private void revert() {
        pos = start;
    }

    private Token save() {
        return tokens.get(pos - 1);
    }

    private boolean accept(Token.Type... valid) throws CompilerException {
        Token t = next();
        for (Token.Type v : valid) {
            if (t.getType() == v) {
                return true;
            }
        }
        backup();
        return false;
    }

    private boolean accept(String... valid) throws CompilerException {
        String t = next().getData();
        for (String v : valid) {
            if (t.equals(v)) {
                return true;
            }
        }
        backup();
        return false;
    }

    private boolean check(Token.Type... valid) throws CompilerException {
        Token t = peek();
        for (Token.Type v : valid) {
            if (t.getType() == v) {
                return true;
            }
        }
        return false;
    }

    private boolean check(String... valid) throws CompilerException {
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

    private boolean expect(String valid) throws CompilerException {
        String t = next().getData();
        if (t.equals(valid)) {
            return true;
        }
        backup();
        error(String.format("expected '%s'", valid));
        return false;
    }
}
