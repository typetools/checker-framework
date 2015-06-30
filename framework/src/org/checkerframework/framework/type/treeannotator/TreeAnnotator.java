package org.checkerframework.framework.type.treeannotator;

import com.sun.source.tree.MethodTree;

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

    /**
     * This method is not called when checking a method invocation against its
     * declaration. So, instead of overriding this method, override
     * TypeAnnotator.visitExecutable. TypeAnnotator.visitExecutable is called
     * both when checking method declarations and method invocations.
     *
     * @see org.checkerframework.framework.type.typeannotator.TypeAnnotator
     */
    @Override
    public Void visitMethod(MethodTree node, AnnotatedTypeMirror p) {
        return super.visitMethod(node, p);
    }

}
