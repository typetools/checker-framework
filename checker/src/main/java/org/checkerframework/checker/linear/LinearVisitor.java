package org.checkerframework.checker.linear;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import javax.lang.model.element.Element;
import org.checkerframework.checker.linear.qual.Linear;
import org.checkerframework.checker.linear.qual.Unusable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A type-checking visitor for the Linear type system. The visitor reports an error ("unsafe.use")
 * for any use of a reference of {@link Unusable} type. In other words, it reports an error for any
 * {@link Linear} references that is used more than once, or is used after it has been "used up".
 *
 * @see LinearChecker
 */
public class LinearVisitor extends BaseTypeVisitor<LinearAnnotatedTypeFactory> {

    public LinearVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /**
     * Return true if the node represents a reference to a local variable or parameter.
     *
     * <p>
     *
     * <p>In Linear Checker, only local variables and method parameters can be of {@link Linear} or
     * {@link Unusable} types.
     *
     * @param node a tree
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

    /** Issue an error if the node represents a reference that has been used up. */
    private void checkLegality(ExpressionTree node) {
        if (isLocalVarOrParam(node)) {
            AnnotatedTypeMirror x = atypeFactory.getAnnotatedType(node);
            if (atypeFactory.getAnnotatedType(node).hasAnnotation(Unusable.class)) {
                checker.reportError(node, "use.unsafe", TreeUtils.elementFromUse(node));
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

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            String errorKey) {
        if (varType.hasAnnotation(Linear.class)) {
            if (valueTree instanceof LiteralTree || valueTree instanceof NewClassTree) {
                valueType.removeAnnotation(atypeFactory.NORMAL);
                valueType.addAnnotation(atypeFactory.LINEAR);
            }
        }
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);
    }

    /** Linear Checker does not contain a rule for method invocation. */
    // Premature optimization:  Don't check method invocability
    @Override
    protected void checkMethodInvocability(
            AnnotatedExecutableType method, MethodInvocationTree node) {}
}
