package org.checkerframework.checker.lock;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.Tree.Kind;

public class LockTreeAnnotator extends TreeAnnotator {

    /** Annotation constants */
    protected final AnnotationMirror GUARDEDBY;

    public LockTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);

        Elements elements = atypeFactory.getElementUtils();
        GUARDEDBY = AnnotationUtils.fromClass(elements, GuardedBy.class);
    }

    @Override
    public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
        if (isBinaryComparisonOperator(node.getKind())) {
            type.replaceAnnotation(GUARDEDBY);

            return null;
        }

        return super.visitBinary(node, type);
    }

    // Indicates that the result of the operation is a boolean value.
    private static boolean isBinaryComparisonOperator(Kind opKind) {
        switch(opKind){
            case EQUAL_TO:
            case NOT_EQUAL_TO:
            case LESS_THAN:
            case LESS_THAN_EQUAL:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL:
                return true;
            default:
        }

        return false;
    }
}
