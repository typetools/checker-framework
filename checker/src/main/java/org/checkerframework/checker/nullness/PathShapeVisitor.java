package org.checkerframework.checker.nullness;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The visitor for the Path Shape Checker. It skips classes that do not use paths, and it checks the
 * initializer of an effectively final local variable against the variable's type, refined from the
 * initializer as described in {@link PathShapeAnnotatedTypeFactory#refineFromInitializer}.
 *
 * @checker_framework.manual #path-shape-checker Path Shape Checker
 */
public class PathShapeVisitor extends BaseTypeVisitor<PathShapeAnnotatedTypeFactory> {

  /**
   * Creates a new PathShapeVisitor.
   *
   * @param checker the checker that uses this visitor
   */
  public PathShapeVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public void processClassTree(ClassTree classTree) {
    if (atypeFactory.shouldCheck(classTree)) {
      super.processClassTree(classTree);
    }
  }

  @Override
  protected boolean commonAssignmentCheck(
      Tree varTree,
      ExpressionTree valueExpTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    // The superclass implementation checks each branch of a conditional expression by calling this
    // method.
    if (!(varTree instanceof VariableTree variableTree)
        || valueExpTree instanceof ConditionalExpressionTree) {
      return super.commonAssignmentCheck(varTree, valueExpTree, errorKey, extraArgs);
    }
    // The type factory cannot refine the type of the declaration itself, because computing the type
    // of some initializers, such as array initializers, requires the type of the declaration.
    AnnotatedTypeMirror varType = atypeFactory.getAnnotatedTypeLhs(varTree);
    atypeFactory.refineFromInitializer(TreeUtils.elementFromDeclaration(variableTree), varType);
    if (!validateType(varTree, varType)) {
      return true;
    }
    return commonAssignmentCheck(varType, valueExpTree, errorKey, extraArgs);
  }
}
