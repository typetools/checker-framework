package checkers.linear;

import javax.lang.model.element.Element;

import com.sun.source.tree.*;

import checkers.basetype.BaseTypeVisitor;
import checkers.linear.quals.Unusable;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.TreeUtils;

/**
 * A type-checking visitor for the Linear type system.  The visitor reports
 * an error ("unsafe.use") for any use of a reference of {@link Unusable}
 * type.  In other words, it reports an error for any {@code Linear}
 * references that is used more than once, or is used after it has been
 * "used up".
 *
 * @see LinearChecker
 */
public class LinearVisitor extends BaseTypeVisitor<LinearChecker> {

    public LinearVisitor(LinearChecker checker, CompilationUnitTree root) {
        super(checker, root);
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
    protected boolean checkMethodInvocability(AnnotatedExecutableType method,
            MethodInvocationTree node) {
        return true;
    }
}
