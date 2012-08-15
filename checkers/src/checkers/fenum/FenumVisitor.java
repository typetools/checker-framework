package checkers.fenum;

import checkers.basetype.BaseTypeVisitor;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.TreeUtils;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;

public class FenumVisitor extends BaseTypeVisitor<FenumChecker> {
    public FenumVisitor(FenumChecker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public Void visitBinary(BinaryTree node, Void p) {
        if (!TreeUtils.isStringConcatenation(node)) {
            // TODO: ignore string concatenations

            AnnotatedTypeMirror lhs = atypeFactory.getAnnotatedType(node.getLeftOperand());
            AnnotatedTypeMirror rhs = atypeFactory.getAnnotatedType(node.getRightOperand());
            if (!(checker.isSubtype(lhs, rhs)
                  || checker.isSubtype(rhs, lhs))) {
                checker.report(Result.failure("binary.type.incompatible", lhs, rhs), node);
            }
        }
        return super.visitBinary(node, p);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        ExpressionTree var = node.getVariable();
        ExpressionTree expr = node.getExpression();
        AnnotatedTypeMirror varType = atypeFactory.getAnnotatedType(var);
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(expr);

        if (!(checker.isSubtype(exprType, varType))) {
            checker.report(Result.failure("compoundassign.type.incompatible", varType, exprType),
                           node);
        }

        return super.visitCompoundAssignment(node, p);
    }

    @Override
    public Void visitSwitch(SwitchTree node, Void p) {
        ExpressionTree expr = node.getExpression();
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(expr);

        for (CaseTree caseExpr : node.getCases()) {
            ExpressionTree realCaseExpr = caseExpr.getExpression();
            if (realCaseExpr != null) {
                AnnotatedTypeMirror caseType = atypeFactory.getAnnotatedType(realCaseExpr);

                this.commonAssignmentCheck(exprType, caseType, caseExpr,
                        "switch.type.incompatible");
            }
        }
        return super.visitSwitch(node, p);
    }

    @Override
    protected boolean checkConstructorInvocation(AnnotatedDeclaredType dt,
            AnnotatedExecutableType constructor, Tree src) {
        // Ignore the default annotation on the constructor
        return true;
    }

    // TODO: should we require a match between switch expression and cases?

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
            AnnotatedDeclaredType useType) {
        // The checker calls this method to compare the annotation used in a
        // type to the modifier it adds to the class declaration. As our default
        // modifier is Unqualified, this results in an error when a non-subtype
        // is used. Can we use FenumTop as default instead?
        return true;
    }

}
