package checkers.units;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;

import checkers.basetype.BaseTypeVisitor;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;

/**
 * Units visitor.
 *
 * Ensure consistent use of compound assignments.
 */
public class UnitsVisitor extends BaseTypeVisitor<UnitsChecker> {
    public UnitsVisitor(UnitsChecker checker, CompilationUnitTree root) {
        super(checker, root);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        ExpressionTree var = node.getVariable();
        ExpressionTree expr = node.getExpression();
        AnnotatedTypeMirror varType = atypeFactory.getAnnotatedType(var);
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(expr);

        Kind kind = node.getKind();

        if ( (kind == Kind.PLUS_ASSIGNMENT || kind == Kind.MINUS_ASSIGNMENT) &&
                !checker.isSubtype(exprType, varType)) {
            checker.report(Result.failure("compoundassign.type.incompatible",
                    varType, exprType), node);
        }

        return super.visitCompoundAssignment(node, p);
    }

}