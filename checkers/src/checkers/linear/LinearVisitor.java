package checkers.linear;

import javax.lang.model.element.Element;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;

import checkers.basetype.BaseTypeChecker;
import checkers.basetype.BaseTypeVisitor;
import checkers.linear.quals.Unusable;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.TreeUtils;

public class LinearVisitor extends BaseTypeVisitor<Void, Void> {

    public LinearVisitor(LinearChecker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    static boolean possibleUnusable(ExpressionTree node) {
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

    private void checkLegality(ExpressionTree node) {
        if (possibleUnusable(node)) {
            if (atypeFactory.getAnnotatedType(node).hasAnnotation(Unusable.class)) {
                checker.report(Result.failure("unsafe.use",
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

    protected boolean checkMethodInvocability(AnnotatedExecutableType method,
            MethodInvocationTree node) {
        return true;
    }
}
