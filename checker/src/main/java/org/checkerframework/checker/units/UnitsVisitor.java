package org.checkerframework.checker.units;

import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import org.checkerframework.checker.units.qual.UnknownUnits;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;

/**
 * Units visitor.
 *
 * <p>Ensure consistent use of compound assignments.
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

        Tree.Kind kind = node.getKind();

        if ((kind == Tree.Kind.PLUS_ASSIGNMENT || kind == Tree.Kind.MINUS_ASSIGNMENT)) {
            if (!atypeFactory
                    .getQualifierHierarchy()
                    .isSubtype(
                            exprType.getEffectiveAnnotations(),
                            varType.getEffectiveAnnotations())) {
                checker.reportError(
                        node, "compound.assignment.type.incompatible", varType, exprType);
            }
        } else if (!exprType.hasAnnotation(UnknownUnits.class)) {
            // Only allow mul/div with unqualified units
            checker.reportError(node, "compound.assignment.type.incompatible", varType, exprType);
        }

        return null; // super.visitCompoundAssignment(node, p);
    }
}
