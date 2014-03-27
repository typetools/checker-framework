package org.checkerframework.checker.linear;

import javax.lang.model.element.Element;

import org.checkerframework.checker.linear.qual.Linear;
import org.checkerframework.checker.linear.qual.Unusable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * A type-checking visitor for the Linear type system.  The visitor reports
 * an error ("unsafe.use") for any use of a reference of {@link Unusable}
 * type.  In other words, it reports an error for any {@code Linear}
 * references that is used more than once, or is used after it has been
 * "used up".
 *
 * @see LinearChecker
 */
public class LinearVisitor extends BaseTypeVisitor<LinearAnnotatedTypeFactory> {

    public LinearVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * Return true if the node represents a reference to a local variable
     * or parameter.
     *
     * In Linear Checker, only local variables and method parameters can be
     * of {@link Linear} or {@link Unusable} types.
     *
     * @param node   a tree
     * @return true if node is a local variable or parameter reference
     */
    static boolean isLocalVarOrParam(ExpressionTree node) {
        Element elem = TreeUtils.elementFromUse(node);
        if (elem == null) return false;
        switch (elem.getKind()) {
        case PARAMETER:
        case LOCAL_VARIABLE:
            return true;
        default:
            return false;
        }
    }

    /**
     * Issue an error if the node represents a reference that has been used up.
     */
    private void checkLegality(ExpressionTree node) {
        if (isLocalVarOrParam(node)) {
            if (atypeFactory.getAnnotatedType(node).hasAnnotation(Unusable.class)) {
                checker.report(Result.failure("use.unsafe",
                        TreeUtils.elementFromUse(node), node), node);
            }
        }
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        checkLegality(node);
        return super.visitIdentifier(node, p);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        checkLegality(node);
        return super.visitMemberSelect(node, p);
    }

    /**
     * Linear Checker does not contain a rule for method invocation.
     */
    // Premature optimization:  Don't check method invocability
    @Override
    protected void checkMethodInvocability(AnnotatedExecutableType method,
            MethodInvocationTree node) {
    }
}
