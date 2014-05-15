package org.checkerframework.framework.type;

import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;

/**
 * {@link TreeAnnotator} is an abstract SimpleTreeVisitor to be
 * used with {@link org.checkerframework.framework.type.ListTreeAnnotator}
 *
 * Extenders must implement the default action
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

    // Extenders must implement the default action
    @Override
    public abstract Void defaultAction(Tree tree, AnnotatedTypeMirror type);
}
