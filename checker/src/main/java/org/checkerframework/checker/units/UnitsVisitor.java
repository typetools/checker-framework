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
  public Void visitCompoundAssignment(CompoundAssignmentTree tree, Void p) {
    ExpressionTree var = tree.getVariable();
    ExpressionTree expr = tree.getExpression();
    AnnotatedTypeMirror varType = atypeFactory.getAnnotatedType(var);
    AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(expr);

    Tree.Kind kind = tree.getKind();

    if ((kind == Tree.Kind.PLUS_ASSIGNMENT || kind == Tree.Kind.MINUS_ASSIGNMENT)) {
      if (!qualHierarchy.isSubtypeShallowEffective(exprType, varType)) {
        checker.reportError(tree, "compound.assignment", varType, exprType);
      }
    } else if (!exprType.hasPrimaryAnnotation(UnknownUnits.class)) {
      // Only allow mul/div with unqualified units
      checker.reportError(tree, "compound.assignment", varType, exprType);
    }

    return null; // super.visitCompoundAssignment(tree, p);
  }
}
