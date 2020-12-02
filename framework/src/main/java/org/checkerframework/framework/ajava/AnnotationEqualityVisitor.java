package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.ArrayList;
import java.util.List;

public class AnnotationEqualityVisitor extends DoubleJavaParserVisitor {
    private boolean annotationsMatch;
    private Node mismatchedNode1;
    private Node mismatchedNode2;

    public AnnotationEqualityVisitor() {
        annotationsMatch = true;
        mismatchedNode1 = null;
        mismatchedNode2 = null;
    }

    public boolean getAnnotationsMatch() {
        return annotationsMatch;
    }

    public Node getMismatchedNode1() {
        return mismatchedNode1;
    }

    public Node getMismatchedNode2() {
        return mismatchedNode2;
    }

    @Override
    public void defaultAction(Node node1, Node node2) {
        if (!(node1 instanceof NodeWithAnnotations<?>)
                || !(node2 instanceof NodeWithAnnotations<?>)) {
            return;
        }

        List<AnnotationExpr> node1Annos = readAnnotations((NodeWithAnnotations<?>) node1);
        List<AnnotationExpr> node2Annos = readAnnotations((NodeWithAnnotations<?>) node2);
        if (!node1Annos.equals(node2Annos)) {
            annotationsMatch = false;
            System.out.println("Got mismatch:");
            System.out.println(node1Annos);
            System.out.println(node2Annos);
            mismatchedNode1 = node1;
            mismatchedNode2 = node2;
        }
    }

    private List<AnnotationExpr> readAnnotations(NodeWithAnnotations<?> node) {
        List<AnnotationExpr> result = new ArrayList<>();
        VoidVisitor<Void> visitor =
                new VoidVisitorAdapter<Void>() {
                    @Override
                    public void visit(LineComment node, Void p) {
                        node.remove();
                    }

                    @Override
                    public void visit(BlockComment node, Void p) {
                        node.remove();
                    }

                    @Override
                    public void visit(JavadocComment node, Void p) {
                        node.remove();
                    }
                };

        for (AnnotationExpr annotation : node.getAnnotations()) {
            AnnotationExpr annotationCopy = annotation.clone();
            annotationCopy.accept(visitor, null);
            result.add(annotationCopy);
        }

        return result;
    }
}
