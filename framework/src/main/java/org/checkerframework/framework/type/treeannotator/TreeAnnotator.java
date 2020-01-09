package org.checkerframework.framework.type.treeannotator;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * {@link TreeAnnotator} is an abstract SimpleTreeVisitor to be used with {@link ListTreeAnnotator}.
 *
 * <p>This class does not visit component parts of the tree. By default, the visit methods all call
 * {@link #defaultAction(Tree, Object)}, which does nothing unless overriden. Therefore, subclass do
 * not need to call super unless they override {@link #defaultAction(Tree, Object)}.
 *
 * @see ListTreeAnnotator
 * @see PropagationTreeAnnotator
 * @see LiteralTreeAnnotator
 */
public abstract class TreeAnnotator extends SimpleTreeVisitor<Void, AnnotatedTypeMirror> {

    protected final AnnotatedTypeFactory atypeFactory;

    public TreeAnnotator(AnnotatedTypeFactory atypeFactory) {
        this.atypeFactory = atypeFactory;
    }

    /**
     * This method is not called when checking a method invocation against its declaration. So,
     * instead of overriding this method, override TypeAnnotator.visitExecutable.
     * TypeAnnotator.visitExecutable is called both when checking method declarations and method
     * invocations.
     *
     * @see org.checkerframework.framework.type.typeannotator.TypeAnnotator
     */
    @Override
    public Void visitMethod(MethodTree node, AnnotatedTypeMirror p) {
        return super.visitMethod(node, p);
    }

    /**
     * When overriding this method, getAnnotatedType on the left and right operands should only be
     * called when absolutely necessary. Otherwise, the checker will be very slow on heavily nested
     * binary trees. (For example, a + b + c + d + e + f + g + h.)
     *
     * <p>If a checker's performance is still too slow, the types of binary trees could be computed
     * in a subclass of {@link org.checkerframework.framework.flow.CFTransfer}. When computing the
     * types in a transfer, look up the value in the store rather than the AnnotatedTypeFactory.
     * Then this method should annotate binary trees with top so that the type applied in the
     * transfer is always a subtype of the type the AnnotatedTypeFactory computes.
     */
    @Override
    public Void visitBinary(BinaryTree node, AnnotatedTypeMirror mirror) {
        return super.visitBinary(node, mirror);
    }
}
