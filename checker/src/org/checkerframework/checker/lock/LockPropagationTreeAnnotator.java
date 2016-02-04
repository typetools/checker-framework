package org.checkerframework.checker.lock;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;

import com.sun.source.tree.BinaryTree;

public class LockPropagationTreeAnnotator extends PropagationTreeAnnotator {

    /** Annotation constants */
    protected final AnnotationMirror GUARDEDBY;

    public LockPropagationTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);

        Elements elements = atypeFactory.getElementUtils();
        GUARDEDBY = AnnotationUtils.fromClass(elements, GuardedBy.class);
    }

    @Override
    public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
        if (((LockAnnotatedTypeFactory)atypeFactory).isBinaryComparisonOperator(node.getKind())) {
            // TODO: Is any defaulting done by super.visitBinary needed here?
            type.replaceAnnotation(GUARDEDBY);

            return null;
        }

        return super.visitBinary(node, type);
    }
}
