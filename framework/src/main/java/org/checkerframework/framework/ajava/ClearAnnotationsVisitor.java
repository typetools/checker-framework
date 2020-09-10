package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/** A visitor that clears all annotations from a JavaParser AST. */
public class ClearAnnotationsVisitor extends VoidVisitorAdapter<Void> {
    @Override
    public void visit(MarkerAnnotationExpr n, Void p) {
        n.remove();
    }

    @Override
    public void visit(NormalAnnotationExpr n, Void p) {
        n.remove();
    }

    @Override
    public void visit(SingleMemberAnnotationExpr n, Void p) {
        n.remove();
    }
}
