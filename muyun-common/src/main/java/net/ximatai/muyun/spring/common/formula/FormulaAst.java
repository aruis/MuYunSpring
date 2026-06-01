package net.ximatai.muyun.spring.common.formula;

import java.util.List;

final class FormulaAst {
    private FormulaAst() {
    }

    enum NodeType {
        VALUE, FIELD, UNARY, BINARY, ASSIGN, FUNC, EMPTY
    }

    static class AstNode {
        NodeType type = NodeType.EMPTY;
    }

    static final class ValueNode extends AstNode {
        final Object value;
        final boolean stringLiteral;

        ValueNode(Object value) {
            this(value, false);
        }

        ValueNode(Object value, boolean stringLiteral) {
            this.type = NodeType.VALUE;
            this.value = value;
            this.stringLiteral = stringLiteral;
        }
    }

    static final class FieldNode extends AstNode {
        final String dataIndex;

        FieldNode(String dataIndex) {
            this.type = NodeType.FIELD;
            this.dataIndex = dataIndex;
        }
    }

    static final class UnaryNode extends AstNode {
        final String op;
        final AstNode arg;

        UnaryNode(String op, AstNode arg) {
            this.type = NodeType.UNARY;
            this.op = op;
            this.arg = arg;
        }
    }

    static final class BinaryNode extends AstNode {
        final String op;
        final AstNode left;
        final AstNode right;

        BinaryNode(String op, AstNode left, AstNode right) {
            this.type = NodeType.BINARY;
            this.op = op;
            this.left = left;
            this.right = right;
        }
    }

    static final class AssignNode extends AstNode {
        final AstNode left;
        final AstNode right;

        AssignNode(AstNode left, AstNode right) {
            this.type = NodeType.ASSIGN;
            this.left = left;
            this.right = right;
        }
    }

    static final class FuncNode extends AstNode {
        final String name;
        final List<AstNode> args;

        FuncNode(String name, List<AstNode> args) {
            this.type = NodeType.FUNC;
            this.name = name;
            this.args = args == null ? List.of() : args;
        }
    }
}
