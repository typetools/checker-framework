package org.checkerframework.framework.type;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
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

    /**
     * This method is not called when checking a method invocation against its
     * declaration. So, instead of overriding this method, override
     * TypeAnnotator.visitExecutable. TypeAnnotator.visitExecutable is called
     * both when checking method declarations and method invocations.
     * 
     * @see org.checkerframework.framework.type.TypeAnnotator
     */
    @Override
    public Void visitMethod(MethodTree node, AnnotatedTypeMirror p) {
        return super.visitMethod(node, p);
    }

}
