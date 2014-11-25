package org.checkerframework.framework.type.treeannotator;

import com.sun.source.util.SimpleTreeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * {@link TreeAnnotator} is an abstract SimpleTreeVisitor to be
 * used with {@link ListTreeAnnotator}
 *
 * @see ListTreeAnnotator
 * @see PropagationTreeAnnotator
 * @see ImplicitsTreeAnnotator
 */
public abstract class TreeAnnotator extends SimpleTreeVisitor<Void, AnnotatedTypeMirror> {

    protected final AnnotatedTypeFactory atypeFactory;

    public TreeAnnotator(AnnotatedTypeFactory atypeFactory) {
        this.atypeFactory = atypeFactory;
    }

}
