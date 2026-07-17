package org.checkerframework.common.basetype;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.TreeUtils;
import org.plumelib.util.CollectionsP;
import org.plumelib.util.IPair;
import org.plumelib.util.UnionFind;

/**
 * The set of expressions a method side-effects, beyond those listed in its {@link SideEffectsOnly}
 * annotation.
 */
public class DisallowedSideEffects {

  /** Creates an empty DisallowedSideEffects. */
  public DisallowedSideEffects() {}

  /** Expressions a method side-effects that are not in its {@link SideEffectsOnly} annotation. */
  protected final List<IPair<Tree, JavaExpression>> exprs = new ArrayList<>(1);

  /**
   * Adds {@code t} and {@code javaExpr} as a pair to this.
   *
   * @param t the expression that is mutated
   * @param javaExpr the corresponding Java expression
   */
  public void addExpr(Tree t, JavaExpression javaExpr) {
    exprs.add(IPair.of(t, javaExpr));
  }

  /**
   * Returns the expressions a method side-effects that are <b>not</b> listed in its {@link
   * SideEffectsOnly} annotation.
   *
   * @return side-effected expressions, beyond what is in {@code @SideEffectsOnly}
   */
  public List<IPair<Tree, JavaExpression>> getExprs() {
    return exprs;
  }

  // Static methods

  /**
   * Returns the computed {@code DisallowedSideEffects}.
   *
   * @param statement the statement to check
   * @param annoProvider the annotation provider
   * @param sideEffectsOnlyExpressions the values in the {@link SideEffectsOnly} annotation
   * @param checker the checker to use
   * @return a DisallowedSideEffects
   */
  public static DisallowedSideEffects checkSideEffectsOnly(
      TreePath statement,
      AnnotationProvider annoProvider,
      List<JavaExpression> sideEffectsOnlyExpressions,
      BaseTypeChecker checker) {
    DisallowedSideEffectsHelper helper =
        new DisallowedSideEffectsHelper(annoProvider, sideEffectsOnlyExpressions, checker);
    helper.scan(statement, null);
    return helper.disallowedSideEffects;
  }

  /**
   * Visitor that collects mutated expressions that are not listed in a {@link SideEffectsOnly}
   * annotation.
   */
  protected static class DisallowedSideEffectsHelper extends TreePathScanner<Void, Void> {
    /** Result computed by DisallowedSideEffectsHelper. */
    DisallowedSideEffects disallowedSideEffects = new DisallowedSideEffects();

    /**
     * List of expressions specified as annotation arguments in a {@link SideEffectsOnly}
     * annotation.
     */
    List<JavaExpression> sideEffectsOnlyExpressionsFromAnnotation;

    UnionFind<JavaExpression> aliasedExpressions =
        new UnionFind<>(null, JavaExpression::containsAsReceiver);

    /**
     * The elements of {@link #aliasedExpressions}. Needed because UnionFind does not
     *
     * <p>/** The annotation provider.
     */
    protected final AnnotationProvider annoProvider;

    /** The checker to use. */
    BaseTypeChecker checker;

    /**
     * Creates a new DisallowedSideEffectsHelper.
     *
     * @param annoProvider the annotation provider
     * @param sideEffectsOnlyExpressions the arguments/values of a {@link SideEffectsOnly}
     *     annotation
     * @param checker the checker to use
     */
    public DisallowedSideEffectsHelper(
        AnnotationProvider annoProvider,
        List<JavaExpression> sideEffectsOnlyExpressions,
        BaseTypeChecker checker) {
      this.annoProvider = annoProvider;
      this.sideEffectsOnlyExpressionsFromAnnotation = sideEffectsOnlyExpressions;
      this.checker = checker;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
      Element invokedElem = TreeUtils.elementFromUse(node);
      if (invokedElem == null || TreeUtils.isEnumSuperCall(node)) {
        return super.visitMethodInvocation(node, aVoid);
      }
      boolean isMarkedPure = annoProvider.getDeclAnnotation(invokedElem, Pure.class) != null;
      boolean isMarkedSideEffectFree =
          annoProvider.getDeclAnnotation(invokedElem, SideEffectFree.class) != null;
      if (isMarkedPure || isMarkedSideEffectFree) {
        // TODO: Should all the checking be integrated together?
        return super.visitMethodInvocation(node, aVoid);
      }

      boolean isInvokedMethodMarkedWithSideEffectsOnly =
          annoProvider.getDeclAnnotation(invokedElem, SideEffectsOnly.class) != null;

      List<JavaExpression> actualSideEffectedExprs =
          this.getJavaExpressionsFromMethodInvocation(node);

      // If the invoked method is NOT marked with @SideEffectsOnly, it may modify anything.
      if (!isInvokedMethodMarkedWithSideEffectsOnly) {
        // What does it modify? Check the arguments for the method invocation.
        if (actualSideEffectedExprs.isEmpty()) {
          // The call has no receiver or arguments, so it might modify arbitrary state.
          checker.reportError(
              node,
              "purity.incorrect.sideeffectsonly",
              invokedElem.getSimpleName(),
              "arbitrary expressions");
        }
      }
      actualSideEffectedExprs.stream()
          .filter(this::isDisallowedSideEffectedExpression)
          .forEach(expr -> disallowedSideEffects.addExpr(node, expr));
      return super.visitMethodInvocation(node, aVoid);
    }

    /**
     * Returns the arguments to a method invocation, including the receiver.
     *
     * @param methodInvok a method invocation
     * @return the arguments to a method invocation, including the receiver
     */
    private List<JavaExpression> getJavaExpressionsFromMethodInvocation(
        MethodInvocationTree methodInvok) {
      // TODO: collect all subexpressions of the given expression.  For now it just considers the
      // actual arguments, which is incomplete.
      List<? extends ExpressionTree> args = methodInvok.getArguments();
      ExpressionTree receiver = TreeUtils.getReceiverTree(methodInvok);
      List<ExpressionTree> exprs;
      if (receiver == null) {
        // Unfortunate, unnecessary copying.
        exprs = new ArrayList<>(args);
      } else {
        exprs = new ArrayList<>(args.size() + 1);
        exprs.add(receiver);
        exprs.addAll(args);
      }
      return CollectionsP.mapList(JavaExpression::fromTree, exprs);
    }

    /**
     * Returns true if the given expression is a side-effected expression beyond what is listed in
     * the {@link SideEffectsOnly} annotation.
     *
     * <ul>
     *   <li>The expression is not listed in the {@link SideEffectsOnly} annotation.
     *   <li>The expression is modifiable by other code.
     *   <li>The expression is <b>not</b> aliased by other variables in the method body.
     * </ul>
     *
     * @param expr the expression to check for side-effecting
     * @return true if the given expression is a side-effected expression beyond what is listed in
     *     the {@link SideEffectsOnly} annotation
     */
    private boolean isDisallowedSideEffectedExpression(JavaExpression expr) {
      if (!expr.isModifiableByOtherCode()) {
        return false;
      }
      JavaExpression exprNonCanonical = expr;
      expr = aliasedExpressions.find(expr);
      for (JavaExpression seOnlyExpr : sideEffectsOnlyExpressionsFromAnnotation) {
        System.out.printf("Testing whether %s contains %s%n", expr, seOnlyExpr);
        aliasedExpressions.add(seOnlyExpr);
        if (aliasedExpressions.test(expr, seOnlyExpr)) {
          System.out.printf("containsAsReceiver: %s %s => false%n", expr, seOnlyExpr);
          return false;
        }
      }
      System.out.printf("isDisallowedSideEffectedExpression => true: %s%n", expr);
      return true;
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void aVoid) {
      JavaExpression lhs = JavaExpression.fromTree(node.getVariable());
      JavaExpression rhs = JavaExpression.fromTree(node.getExpression());
      // TODO: Need to check for subexpressions, in case the `@SideEffectsOnly(...)` expressions are
      // broader than `lhs`.
      if (!sideEffectsOnlyExpressionsFromAnnotation.contains(lhs)) {
        disallowedSideEffects.addExpr(node, lhs);
      }
      aliasedExpressions.union(lhs, rhs);
      return super.visitAssignment(node, aVoid);
    }

    @Override
    public Void visitVariable(VariableTree node, Void aVoid) {
      ExpressionTree initializer = node.getInitializer();
      if (initializer == null) {
        // A declaration with no initializer, such as `int x;`, creates no alias.
        return super.visitVariable(node, aVoid);
      }
      JavaExpression name = JavaExpression.fromVariableTree(node);
      JavaExpression expr = JavaExpression.fromTree(initializer);
      addAlias(name, expr);
      return super.visitVariable(node, aVoid);
    }

    @Override
    public Void visitUnary(UnaryTree node, Void aVoid) {
      switch (node.getKind()) {
        case POSTFIX_INCREMENT, POSTFIX_DECREMENT, PREFIX_INCREMENT, PREFIX_DECREMENT -> {
          JavaExpression operand = JavaExpression.fromTree(node.getExpression());
          // TODO: Need to check for subexpressions, in case the `@SideEffectsOnly(...)`
          // expressions are broader than `operand`.
          if (!sideEffectsOnlyExpressionsFromAnnotation.contains(operand)) {
            disallowedSideEffects.addExpr(node, operand);
          }
        }
        default -> {}
      }
      return super.visitUnary(node, aVoid);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void aVoid) {
      // Does not make the left-hand side an alias of the right-hand side,
      // because the rhs expression uses the lhs.
      JavaExpression lhs = JavaExpression.fromTree(node.getVariable());
      // TODO: Need to check for subexpressions, in case the `@SideEffectsOnly(...)` expressions are
      // broader than `lhs`.
      if (!sideEffectsOnlyExpressionsFromAnnotation.contains(lhs)) {
        disallowedSideEffects.addExpr(node, lhs);
      }
      return super.visitCompoundAssignment(node, aVoid);
    }

    /**
     * Set the two expressions as possibly aliased.
     *
     * @param lhs a Java expression
     * @param rhs a Java expression
     */
    private void addAlias(JavaExpression lhs, JavaExpression rhs) {
      System.out.printf("addAlias(%s, %s) pre: %s%n", lhs, rhs, aliasedExpressions);
      aliasedExpressions.add(lhs);
      aliasedExpressions.add(rhs);
      aliasedExpressions.union(lhs, rhs);
      System.out.printf("addAlias(%s, %s) => %s%n", lhs, rhs, aliasedExpressions);
    }
  }
}
