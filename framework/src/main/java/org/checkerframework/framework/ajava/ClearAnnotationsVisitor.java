package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import java.util.ArrayList;

/** A visitor that clears all annotations from a JavaParser AST. */
public class ClearAnnotationsVisitor extends VoidVisitorWithDefaultAction {
    @Override
    public void defaultAction(Node node) {
        for (Node child : new ArrayList<>(node.getChildNodes())) {
            if (child instanceof AnnotationExpr) {
                node.remove(child);
            }
        }
    }
}
