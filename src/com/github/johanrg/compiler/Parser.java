package com.github.johanrg.compiler;

import com.github.johanrg.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author johan
 * @since 2016-07-02.
 */
public class Parser extends CompilerErrorHandler {
    private final List<Token> tokens;
    private final Identifiers identifiers = new Identifiers();
    private final Stack<ASTOperator> operatorStack = new Stack<>();
    private final Stack<ASTNode> expressionStack = new Stack<>();
    private int start = 0;
    private int pos = 0;
    private int currentScopeLevel = -1;

    public Parser(List<Token> tokens) throws CompilerException {
        this.tokens = tokens;
        identifiers.newScope();
        parseScope();
    }

    private ASTNode parseStatement() throws CompilerException {
        ASTNode node;
        if (checkIfDeclaration()) {
            node = parseIdentifierDeclarationStatement();
        } else if (check(Symbols.Keyword.IF)) {
            node = parseIfStatement();
        } else if (check(Symbols.Keyword.WHILE)) {
            node = parseWhileStatement();
        } else if (check(Symbols.Keyword.FOR)) {
            if (checkIfStandardForStatement()) {
                node = parseForStatement();
            } else {
                node = parseForRangeStatement();
            }
        } else {
            node = parseExpression();
            expect(Token.Type.END_OF_STATEMENT);
        }
        return node;
    }

    private ASTScope parseScope() throws CompilerException {
        List<ASTNode> statements = new ArrayList<>();
        ++currentScopeLevel;
        while (!eof() && scopeDiff(0)) {
            statements.add(parseStatement());
            if (scopeDiff(1)) {
                identifiers.newScope();
                statements.add(parseScope());
            }
        }
        --currentScopeLevel;
        return new ASTScope(statements, identifiers.popScope());
    }

    private ASTNode parseIdentifierDeclarationStatement() throws CompilerException {
        DataType identifierDataType;
        expect(Token.Type.IDENTIFIER);
        Token identifierToken = save();

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
        // NOTE(Johan): double :: is constant value
        if (accept(":")) {
            if (checkIfFunction()) {
                // NOTE(Johan): read in parameter list
                expect("(");
                List<ASTNode> parameters = new ArrayList<>();
                while (!check(")") & !check(Token.Type.END_OF_STATEMENT, Token.Type.EOF)) {
                    parameters.add(parseIdentifierDeclarationStatement());
                    while (accept(",")) {
                        parameters.add(parseIdentifierDeclarationStatement());
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
                    identifiers.newScope();
                    ASTScope compoundStatement = parseScope();
                    ASTFunction function = new ASTFunction(identifierToken.getData(), parameters, compoundStatement,
                            returnType, identifierToken.getLocation());
                    if (!identifiers.addIdentifier(function)) {
                        error(String.format("'%s' is already declared in this scope.", identifierToken.getData()),
                                identifierToken.getLocation());
                    }
                    return function;
                } else {
                    System.out.println("invalid scope");
                }
            } else {
                // NOTE(Johan): Constant identifier
                ASTNode node = parseExpression();
                expect(Token.Type.END_OF_STATEMENT);
                return assignExpressionToIdentifierDeclaration(ASTOperator.Type.ASSIGNMENT, identifierToken, dataTypeToken,
                        identifierDataType, node, true);
            }
            // NOTE(Johan): variable
        } else if (accept("=")) {
            ASTNode node = parseExpression();
            if (!check(",")) {
                expect(Token.Type.END_OF_STATEMENT);
            }
            return assignExpressionToIdentifierDeclaration(ASTOperator.Type.ASSIGNMENT, identifierToken, dataTypeToken,
                    identifierDataType, node, false);
        } else {
            error("data type auto with no expression");
        }
        return null;
    }

    private ASTNode parseExpression() throws CompilerException {
        boolean operator = false;
        boolean previousWasVariable = false; // NOTE(Johan): used exclusively to keep track of unary pre/post increment/decrement
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
                previousWasVariable = false;
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
                previousWasVariable = false;
                Token token = save();
                expressionStack.push(new ASTLiteral(makeLiteralType(token), token.getDataType(), token.getLocation()));
            }

            if (accept(Token.Type.IDENTIFIER)) {
                Token token = save();
                Identifier identifier = identifiers.find((token.getData()));
                if (identifier == null) {
                    error("unknown identifier");
                } else {
                    if (identifier instanceof ASTVariable) {
                        previousWasVariable = true;
                        expressionStack.push((ASTVariable) identifier);
                    } else if (identifier instanceof ASTFunction) {
                        expressionStack.push((ASTFunction) identifier);
                    } else {
                        error(String.format("not a valid type '%s'", token.getData()), token.getLocation());
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
                // NOTE(Johan): special case, the ASTOperator.match will only find unary pre increment/decrement since
                // it doesn't understand context. We need to take care of that here and while we're at it, do some extra
                // syntax check.
                if (op.getGroup() == ASTOperator.Group.UNARY) {
                    Token token = peek();
                    Identifier identifier = identifiers.find(token.getData());
                    boolean nextIsVariable = identifier != null && identifier instanceof ASTVariable;

                    if (op == ASTOperator.Type.UNARY_PRE_INCREMENT) {
                        if (previousWasVariable) {
                            op = ASTOperator.Type.UNARY_POST_INCREMENT;
                        } else if (!nextIsVariable) {
                            error("unary increment can only be used with variables");
                        }
                    } else if (op == ASTOperator.Type.UNARY_PRE_DECREMENT) {
                        if (previousWasVariable) {
                            op = ASTOperator.Type.UNARY_POST_DECREMENT;
                        } else if (!nextIsVariable) {
                            error("unary decrement can only be used with variables");
                        }
                    }
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
                previousWasVariable = false;
                if (op.getAssociativity() == ASTOperator.Associativity.LEFT_TO_RIGHT) {
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
        return Expression.simplifyExpression(expressionStack.pop());
    }

    private ASTNode parseIfStatement() throws CompilerException {
        expect(Symbols.Keyword.IF);
        Token ifToken = save();
        ASTNode expression = parseExpression();
        if (Expression.typeCheck(expression) != DataType.BOOLEAN) {
            error("expected boolean expression", expression.getLocation());
        }
        expect(Token.Type.END_OF_STATEMENT);
        eofNotExpected();
        identifiers.newScope();
        ASTScope ifScope = parseScope();
        if (ifScope.getStatements().size() == 0) {
            error("if scope expected");
        }
        ASTScope elseScope = null;
        if (accept(Symbols.Keyword.ELSE)) {
            expect(Token.Type.END_OF_STATEMENT);
            eofNotExpected();
            identifiers.newScope();
            elseScope = parseScope();
            if (elseScope.getStatements().size() == 0) {
                error("else scope expected");
            }
        }
        return new ASTIfStatement(expression, ifScope, elseScope, ifToken.getLocation());
    }

    private ASTWhileStatement parseWhileStatement() throws CompilerException {
        expect(Symbols.Keyword.WHILE);
        Token whileToken = save();
        ASTNode expression = parseExpression();
        if (Expression.typeCheck(expression) != DataType.BOOLEAN) {
            error("expected boolean expression", expression.getLocation());
        }
        expect(Token.Type.END_OF_STATEMENT);
        eofNotExpected();
        identifiers.newScope();
        ASTScope whileScope = parseScope();
        return new ASTWhileStatement(expression, whileScope, whileToken.getLocation());
    }

    private ASTForStatement parseForStatement() throws CompilerException {
        ASTNode init = null;
        ASTNode condition = null;
        ASTNode increment = null;

        expect(Symbols.Keyword.FOR);
        Token ifToken = save();
        identifiers.newScope();
        if (checkIfDeclaration()) {
            init = parseIdentifierDeclarationStatement();
            if (!(init instanceof ASTBinaryOperator) || !(((ASTBinaryOperator) init).getLeft() instanceof ASTVariable)) {
                error("expected variable declaration");
            }
        }
        expect(",");
        if (!check(",")) {
            condition = parseExpression();
            if (Expression.typeCheck(condition) != DataType.BOOLEAN) {
                error("expected boolean expression", condition.getLocation());
            }
        }
        expect(",");
        if (!check(Token.Type.END_OF_STATEMENT)) {
            increment = parseExpression();
            expect(Token.Type.END_OF_STATEMENT);
        }
        ASTScope forScope = parseScope();
        return new ASTForStatement(init, condition, increment, forScope, ifToken.getLocation());
    }

    private ASTForRangeStatement parseForRangeStatement() throws CompilerException {
        expect(Symbols.Keyword.FOR);
        Token forToken = save();
        Token identifierToken = null;
        if (accept(Token.Type.IDENTIFIER)) {
            identifierToken = save();
            expect(":");
        }
        ASTNode range = parseExpression();
        DataType rangeType = Expression.typeCheck(range);
        ASTVariable identifier;
        if (identifierToken == null) {
            identifier = new ASTVariable("it", rangeType, forToken.getLocation());
        } else {
            identifier = new ASTVariable(identifierToken.getData(), rangeType, identifierToken.getLocation());
        }
        identifiers.newScope();
        identifiers.addIdentifier(identifier);
        ASTNode result = new ASTBinaryOperator(ASTOperator.Type.ASSIGNMENT, identifier, range, identifier.getLocation());
        ASTScope forScope = parseScope();
        return new ASTForRangeStatement(result, forScope, forToken.getLocation());
    }

    private ASTNode assignExpressionToIdentifierDeclaration(ASTOperator.Type assignmentType, Token identifierToken,
                                                            Token dataTypeToken, DataType identifierDataType,
                                                            ASTNode expression, boolean constant) throws CompilerException {
        ASTNode result = null;
        identifierDataType = Expression.typeCheckVsDataType(expression, dataTypeToken, identifierDataType);

        Identifier identifier;
        if (constant) {
            identifier = new ASTConstant(identifierToken.getData(), identifierDataType,
                    identifierToken.getLocation());
        } else {
            identifier = new ASTVariable(identifierToken.getData(), identifierDataType,
                    identifierToken.getLocation());
        }
        if (identifiers.addIdentifier(identifier)) {
            result = new ASTBinaryOperator(assignmentType,
                    (ASTNode) identifier,
                    expression, identifierToken.getLocation());
        } else {
            error(String.format("'%s' is already declared in this scope.", identifierToken.getData()),
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
        } else {
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
        returnToBookmark();
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
        returnToBookmark();
        return isAssignment;
    }

    private boolean checkIfFunction() throws CompilerException {
        bookmark();
        boolean isFunction = accept("(") && accept(Token.Type.IDENTIFIER) && accept(":");
        returnToBookmark();
        if (!isFunction) {
            isFunction = accept("(") && accept(")");
            returnToBookmark();
        }
        return isFunction;
    }

    private boolean checkIfStandardForStatement() throws CompilerException {
        boolean isStandardFor = false;
        bookmark();
        if (accept(Symbols.Keyword.FOR)) {
            if (accept(Token.Type.IDENTIFIER) && accept(":")) {
                if (accept("=")) {
                    parseExpression();
                    if (accept(",")) {
                        isStandardFor = true;
                    }
                }
            } else if (accept(",")) {
                isStandardFor = true;
            }
        }
        returnToBookmark();
        return isStandardFor;
    }

    private void error(String error) throws CompilerException {
        error(error, peek().getLocation());
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

    private void eofNotExpected() throws CompilerException {
        if (check(Token.Type.EOF)) {
            error("unexpected end of file");
        }
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

    private void returnToBookmark() {
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

    private boolean accept(Symbols.Keyword valid) throws CompilerException {
        Token t = next();
        if (t.getKeyword() == valid) {
            return true;
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

    private boolean check(Symbols.Keyword valid) throws CompilerException {
        return peek().getKeyword() == valid;
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

    private boolean expect(Symbols.Keyword valid) throws CompilerException {
        Token t = next();
        if (t.getKeyword() == valid) {
            return true;
        }
        backup();
        error(String.format("expected '%s", valid.toString().toLowerCase()));
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
