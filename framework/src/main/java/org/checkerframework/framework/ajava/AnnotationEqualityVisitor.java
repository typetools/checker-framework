package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;

public class AnnotationEqualityVisitor extends DoubleJavaParserVisitor {
    private boolean annotationsMatch;
    private NodeWithAnnotations<?> mismatchedNode1;
    private NodeWithAnnotations<?> mismatchedNode2;

    public AnnotationEqualityVisitor() {
        annotationsMatch = true;
        mismatchedNode1 = null;
        mismatchedNode2 = null;
    }

    public boolean getAnnotationsMatch() {
        return annotationsMatch;
    }

    public NodeWithAnnotations<?> getMismatchedNode1() {
        return mismatchedNode1;
    }

    public NodeWithAnnotations<?> getMismatchedNode2() {
        return mismatchedNode2;
    }

    @Override
    public void defaultAction(Node node1, Node node2) {
        if (!(node1 instanceof NodeWithAnnotations<?>)
                || !(node2 instanceof NodeWithAnnotations<?>)) {
            return;
        }

        Node node1Copy = node1.clone();
        Node node2Copy = node2.clone();
        for (Comment comment : node1Copy.getAllContainedComments()) {
            comment.remove();
        }

        for (Comment comment : node2Copy.getAllContainedComments()) {
            comment.remove();
        }

        if (!((NodeWithAnnotations<?>) node1Copy)
                .getAnnotations()
                .equals(((NodeWithAnnotations<?>) node2Copy).getAnnotations())) {
            System.out.println("Sizes: ");
            System.out.println(((NodeWithAnnotations<?>) node1Copy).getAnnotations().size());
            System.out.println(((NodeWithAnnotations<?>) node2Copy).getAnnotations().size());
            annotationsMatch = false;
            mismatchedNode1 = (NodeWithAnnotations<?>) node1;
            mismatchedNode2 = (NodeWithAnnotations<?>) node2;
        }
    }
}
