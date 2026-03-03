package org.checkerframework.checker.fenum;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TreeUtilsAfterJava11.CaseUtils;

/** The visitor for Fenum Checker. */
public class FenumVisitor extends BaseTypeVisitor<FenumAnnotatedTypeFactory> {

  /**
   * Creates a Fenum Visitor.
   *
   * @param checker the checker
   */
  public FenumVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public Void visitBinary(BinaryTree tree, Void p) {
    if (!TreeUtils.isStringConcatenation(tree)) {
      // The Fenum Checker is only concerned with primitive types, so just check that
      // the primary annotations are equivalent.
      AnnotatedTypeMirror lhs = atypeFactory.getAnnotatedType(tree.getLeftOperand());
      AnnotatedTypeMirror rhs = atypeFactory.getAnnotatedType(tree.getRightOperand());

      if (!(typeHierarchy.isSubtypeShallowEffective(lhs, rhs)
          || typeHierarchy.isSubtypeShallowEffective(rhs, lhs))) {
        checker.reportError(tree, "binary", lhs, rhs);
      }
    }
    return super.visitBinary(tree, p);
  }

  @Override
  public Void visitSwitch(SwitchTree tree, Void p) {
    ExpressionTree expr = tree.getExpression();
    AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(expr);

    for (CaseTree caseExpr : tree.getCases()) {
      List<? extends ExpressionTree> realCaseExprs = CaseUtils.getExpressions(caseExpr);
      // Check all the case options against the switch expression type:
      for (ExpressionTree realCaseExpr : realCaseExprs) {
        AnnotatedTypeMirror caseType = atypeFactory.getAnnotatedType(realCaseExpr);

        // There is currently no "switch" message key, so it is treated
        // identically to "type.incompatible".
        this.commonAssignmentCheck(exprType, caseType, caseExpr, "type.incompatible");
      }
    }
    return super.visitSwitch(tree, p);
  }

  @Override
  protected void checkConstructorInvocation(
      AnnotatedDeclaredType dt, AnnotatedExecutableType constructor, NewClassTree src) {
    // Ignore the default annotation on the constructor
  }

  @Override
  protected void checkConstructorResult(
      AnnotatedExecutableType constructorType, ExecutableElement constructorElement) {
    // Skip this check
  }

  @Override
  protected AnnotationMirrorSet getExceptionParameterLowerBoundAnnotations() {
    return new AnnotationMirrorSet(atypeFactory.FENUM_UNQUALIFIED);
  }

  // TODO: should we require a match between switch expression and cases?

  @Override
  public boolean isValidUse(
      AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
    // The checker calls this method to compare the annotation used in a type to the modifier it
    // adds to the class declaration. As our default modifier is FenumBottom, this results in an
    // error when a non-subtype is used. Can we use FenumTop as default instead?
    return true;
  }
}
