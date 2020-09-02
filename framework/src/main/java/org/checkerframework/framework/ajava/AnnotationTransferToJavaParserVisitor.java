package org.checkerframework.framework.ajava;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.sun.source.tree.Tree;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.AnnotationUtils;

@SuppressWarnings({"UnusedVariable", "UnusedMethod"})
public class AnnotationTransferToJavaParserVisitor extends JointVisitorWithDefaults {
    /** Factory for obtaining annotations on trees. */
    private AnnotatedTypeFactory aTypeFactory;

    /**
     * The annotations that were obtained from javac trees and added to the JavaParser AST. Each
     * annotation in this set must have an import statement.
     */
    private Set<AnnotationMirror> annotations;

    public AnnotationTransferToJavaParserVisitor(AnnotatedTypeFactory aTypeFactory) {
        this.aTypeFactory = aTypeFactory;
        annotations = AnnotationUtils.createAnnotationSet();
    }

    @Override
    public void defaultAction(Tree javacTree, Node javaParserNode) {
        if (javaParserNode instanceof NodeWithAnnotations) {}
    }

    private AnnotationExpr annotationMirrorToAnnotationExpr(AnnotationMirror annotation) {
        return null;
    }
}
