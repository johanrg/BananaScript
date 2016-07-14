package com.github.johanrg.frontend;

import com.github.johanrg.ast.*;

/**
 * @author johan
 * @since 2016-07-08.
 */
class Expression extends CompilerErrorHandler {
    static DataType typeCheck(ASTNode node) throws CompilerException {
        if (node instanceof Type) {
            return ((Type) node).getDataType();
        } else if (node instanceof ASTUnaryOperator) {
            return typeCheck(((ASTUnaryOperator) node).getSingleNode());
        } else if (node instanceof ASTBinaryOperator) {
            DataType left = typeCheck(((ASTBinaryOperator) node).getLeft());
            DataType right = typeCheck(((ASTBinaryOperator) node).getRight());
            if (left != null && !left.equals(right)) {
                error("type mismatch", node.getLocation());
            }
            if (((ASTBinaryOperator) node).getType().getGroup() == ASTOperator.Group.RELATIONAL) {
                return DataType.BOOLEAN;
            } else {
                return left;
            }
        }

        assert false : "Node not supported";
        return null;
    }

    static DataType typeCheckVsDataType(ASTNode node, Token dataTypeToken, DataType dataType)
            throws CompilerException {
        DataType expressionDataType = Expression.typeCheck(node);
        if (dataType == DataType.AUTO) {
            dataType = expressionDataType;
        } else if (dataType == DataType.VOID) {
            error("data type can not be void", dataTypeToken.getLocation());
        } else if (dataType != expressionDataType) {
            error(String.format("expected expression of type: '%s", dataType.toString().toLowerCase()),
                    node.getLocation());
        }
        return dataType;
    }

    static ASTNode simplifyExpression(ASTNode node) throws CompilerException {
        if (node == null) return null;

        ASTNode result = simplifyExpressionIfPossible(node);
        if (result != null) {
            return result;
        } else {
            return node;
        }
    }

    private static ASTLiteral simplifyExpressionIfPossible(ASTNode node) throws CompilerException {
        if (node instanceof ASTFunction) {
            return null;
        } else if (node instanceof ASTVariable) {
            return null;
        } else if (node instanceof ASTLiteral) {
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

    private static ASTLiteral solveAdd(ASTLiteral left, ASTLiteral right) throws CompilerException {
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
                error("binary additon not allwoed with char type", left.getLocation());
                break;
            case STRING:
                return new ASTLiteral(left.getString() + right.getString(), DataType.STRING, left.getLocation());
            default:
                assert false : "binary add does not support this type.";
        }
        return null;
    }

    private static ASTLiteral solveSub(ASTLiteral left, ASTLiteral right) throws CompilerException {
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
                error("binary subtractions not allowed with char", left.getLocation());
                break;
            case STRING:
                error("binary subtractions not allowed with string", left.getLocation());
                break;
            default:
                assert false : "binary subtraction does not support this type";
        }
        return null;
    }

    private static ASTLiteral solveMul(ASTLiteral left, ASTLiteral right) throws CompilerException {
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
                error("binary multiplication not allowed with char", left.getLocation());
                break;
            case STRING:
                error("binary multiplication not allowed with string", left.getLocation());
                break;
            default:
                assert false : "binary multiplication does not support this type";
        }
        return null;
    }

    private static ASTLiteral solveDiv(ASTLiteral left, ASTLiteral right) throws CompilerException {
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
                error("binary division not allowed with char", left.getLocation());
                break;
            case STRING:
                error("binary division not allowed with string", left.getLocation());
                break;
            default:
                assert false : "binary division does not support this type";
        }
        return null;
    }

    private static ASTLiteral solveMod(ASTLiteral left, ASTLiteral right) throws CompilerException {
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
                error("binary modulus not allowed with char", left.getLocation());
                break;
            case STRING:
                error("binary modulus not allowed with string", left.getLocation());
                break;
            default:
                assert false : "binary modulus does not support this type";
        }
        return null;
    }

    private static ASTLiteral solvePow(ASTLiteral left, ASTLiteral right) throws CompilerException {
        switch (left.getDataType()) {
            case BOOLEAN:
                error("binary exponent not allowed with boolean type", left.getLocation());
                break;
            case INT:
                return new ASTLiteral((int) Math.pow((double) left.getInt(), (double) right.getInt()), DataType.INT,
                        left.getLocation());
            case FLOAT:
                return new ASTLiteral((float) Math.pow(left.getFloat(), right.getFloat()), DataType.FLOAT,
                        left.getLocation());
            case DOUBLE:
                return new ASTLiteral(Math.pow(left.getDouble(), right.getDouble()), DataType.DOUBLE, left.getLocation());
            case CHAR:
                error("binary exponent not allowed with char", left.getLocation());
                break;
            case STRING:
                error("binary exponent not allowed with string", left.getLocation());
                break;
            default:
                assert false : "binary exponent does not support this type";
        }
        return null;
    }
}
