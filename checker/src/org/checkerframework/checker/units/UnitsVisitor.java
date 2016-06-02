package org.checkerframework.checker.units;

import org.checkerframework.checker.units.qual.UnknownUnits;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;

/**
 * Units visitor.
 *
 * Ensure consistent use of compound assignments.
 */
public class UnitsVisitor extends BaseTypeVisitor<UnitsAnnotatedTypeFactory> {
    public UnitsVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        ExpressionTree var = node.getVariable();
        ExpressionTree expr = node.getExpression();
        AnnotatedTypeMirror varType = atypeFactory.getAnnotatedType(var);
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(expr);

        Kind kind = node.getKind();

        if ( (kind == Kind.PLUS_ASSIGNMENT || kind == Kind.MINUS_ASSIGNMENT)) {
            if (!atypeFactory.getTypeHierarchy().isSubtype(exprType, varType)) {
                checker.report(Result.failure("compound.assignment.type.incompatible",
                        varType, exprType), node);
            }
        } else if (exprType.getAnnotation(UnknownUnits.class) == null) {
            // Only allow mul/div with unqualified units
            checker.report(Result.failure("compound.assignment.type.incompatible",
                    varType, exprType), node);
        }

        return null; // super.visitCompoundAssignment(node, p);
    }

}
