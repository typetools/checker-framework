package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class StringLiteralCombineVisitor extends VoidVisitorAdapter<Void> {
    @Override
    public void visit(BinaryExpr node, Void p) {
        super.visit(node, p);

        if (node.getOperator() == BinaryExpr.Operator.PLUS
                && node.getRight().isStringLiteralExpr()) {
            String right = node.getRight().asStringLiteralExpr().asString();
            if (node.getLeft().isStringLiteralExpr()) {
                String left = node.getLeft().asStringLiteralExpr().asString();
                node.replace(new StringLiteralExpr(left + right));
            } else if (node.getLeft().isBinaryExpr()) {
                BinaryExpr leftExpr = node.getLeft().asBinaryExpr();
                if (leftExpr.getOperator() == BinaryExpr.Operator.PLUS
                        && leftExpr.getRight().isStringLiteralExpr()) {
                    String left = leftExpr.getRight().asStringLiteralExpr().asString();
                    node.replace(
                            new BinaryExpr(
                                    leftExpr.getLeft(),
                                    new StringLiteralExpr(left + right),
                                    BinaryExpr.Operator.PLUS));
                }
            }
        }
    }
}
