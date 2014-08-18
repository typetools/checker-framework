package org.checkerframework.framework.type;

import com.sun.source.util.SimpleTreeVisitor;

/**
 * {@link TreeAnnotator} is an abstract SimpleTreeVisitor to be
 * used with {@link org.checkerframework.framework.type.ListTreeAnnotator}
 *
 * @see org.checkerframework.framework.type.ListTreeAnnotator
 * @see org.checkerframework.framework.type.PropagationTreeAnnotator
 * @see org.checkerframework.framework.type.ImplicitsTreeAnnotator
 */
public abstract class TreeAnnotator extends SimpleTreeVisitor<Void, AnnotatedTypeMirror> {

    protected final AnnotatedTypeFactory atypeFactory;

    public TreeAnnotator(AnnotatedTypeFactory atypeFactory) {
        this.atypeFactory = atypeFactory;
    }

}
