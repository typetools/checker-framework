package checkers.units;

import checkers.basetype.BaseTypeVisitor;
import checkers.quals.Unqualified;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;

/**
 * Units visitor.
 *
 * Ensure consistent use of compound assignments.
 */
public class UnitsVisitor extends BaseTypeVisitor<UnitsChecker, UnitsAnnotatedTypeFactory> {
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

        if ( (kind == Kind.PLUS_ASSIGNMENT || kind == Kind.MINUS_ASSIGNMENT)) {
            if (!checker.getTypeHierarchy().isSubtype(exprType, varType)) {
                checker.report(Result.failure("compound.assignment.type.incompatible",
                        varType, exprType), node);
            }
        } else if (exprType.getAnnotation(Unqualified.class) == null) {
            // Only allow mul/div with unqualified units
            checker.report(Result.failure("compound.assignment.type.incompatible",
                    varType, exprType), node);
        }

        return null; // super.visitCompoundAssignment(node, p);
    }

}