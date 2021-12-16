package org.checkerframework.javacutil;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreeScanner;
import java.util.List;
import java.util.function.BiFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A class that visits each result expression of a switch expression and calls {@link
 * #visitSwitchResultExpression(ExpressionTree, Object)} on each result expression. The results of
 * these method calls are combined using {@link #combineResults(Object, Object)}. Call {@link
 * #scanSwitchExpression(Tree, Object)} to start scanning the switch expression.
 *
 * <p>{@link FunctionalSwitchExpressionScanner} can be used to pass functions for to use for {@link
 * #visitSwitchResultExpression(ExpressionTree, Object)} and {@link #combineResults(Object,
 * Object)}.
 *
 * @param <R> the type of the result of {@link #visitSwitchResultExpression(ExpressionTree, Object)}
 * @param <P> the type of the parameter to pass to {@link
 *     #visitSwitchResultExpression(ExpressionTree, Object)}
 */
public abstract class SwitchExpressionScanner<R, P> extends TreeScanner<R, P> {

  /**
   * This method is called for each result expression of the switch expression passed in {@link
   * #scanSwitchExpression(Tree, Object)}.
   *
   * @param resultExpressionTree a result expression of the switch expression currently being
   *     scanned
   * @param p a parameter
   * @return the result of visiting the result expression
   */
  protected abstract R visitSwitchResultExpression(ExpressionTree resultExpressionTree, P p);

  /**
   * This method combines the result of two calls to {@link
   * #visitSwitchResultExpression(ExpressionTree, Object)} or {@code null} and the result of one
   * call to {@link #visitSwitchResultExpression(ExpressionTree, Object)}.
   *
   * @param r1 a possibly null result returned by {@link
   *     #visitSwitchResultExpression(ExpressionTree, Object)}
   * @param r2 a possibly null result returned by {@link
   *     #visitSwitchResultExpression(ExpressionTree, Object)}
   * @return the combination of {@code r1} and {@code r2}
   */
  protected abstract R combineResults(@Nullable R r1, @Nullable R r2);

  /**
   * Scans the given switch expression and calls {@link #visitSwitchResultExpression(ExpressionTree,
   * Object)} on each result expression of the switch expression. {@link #combineResults(Object,
   * Object)} is called to combine the results of visiting multiple switch result expressions.
   *
   * @param switchExpression a switch expression tree
   * @param p the parameter to pass to {@link #visitSwitchResultExpression(ExpressionTree, Object)}
   * @return the result of calling {@link #visitSwitchResultExpression(ExpressionTree, Object)} on
   *     each result expression of {@code switchExpression} and combining the results using {@link
   *     #combineResults(Object, Object)}
   */
  public R scanSwitchExpression(Tree switchExpression, P p) {
    assert switchExpression.getKind().name().equals("SWITCH_EXPRESSION");
    List<? extends CaseTree> caseTrees = TreeUtils.switchExpressionTreeGetCases(switchExpression);
    R result = null;
    for (CaseTree caseTree : caseTrees) {
      if (caseTree.getStatements() != null) {
        // This case is a switch labeled statement group, so scan the statements for yield
        // statements.
        result = combineResults(result, yieldVisitor.scan(caseTree.getStatements(), p));
      } else {
        @SuppressWarnings(
            "nullness:assignment") // caseTree.getStatements() == null, so the case has a body.
        @NonNull Tree body = TreeUtils.caseTreeGetBody(caseTree);
        // This case is a switch rule, so its body is either an expression, block, or throw.
        // See https://docs.oracle.com/javase/specs/jls/se17/html/jls-15.html#jls-15.28.2.
        if (body.getKind() == Kind.BLOCK) {
          // Scan for yield statements.
          result = combineResults(result, yieldVisitor.scan(((BlockTree) body).getStatements(), p));
        } else if (body.getKind() != Kind.THROW) {
          // The expression is the result expression.
          ExpressionTree expressionTree = (ExpressionTree) body;
          result = combineResults(result, visitSwitchResultExpression(expressionTree, p));
        }
      }
    }
    @SuppressWarnings(
        "nullness:assignment" // switch expressions must have at least one case that results in a
    // value, so {@code result} must be nonnull.
    )
    @NonNull R nonNullResult = result;
    return nonNullResult;
  }

  /**
   * A scanner that visits all the yield trees in a given tree and calls {@link
   * #visitSwitchResultExpression(ExpressionTree, Object)} on the expression in the yield trees. It
   * does not descend into switch expressions.
   */
  protected YieldVisitor yieldVisitor = new YieldVisitor();

  /**
   * A scanner that visits all the yield trees in a given tree and calls {@link
   * #visitSwitchResultExpression(ExpressionTree, Object)} on the expression in the yield trees. It
   * does not descend into switch expressions.
   */
  protected class YieldVisitor extends TreeScanner<@Nullable R, P> {

    @Override
    public @Nullable R scan(Tree tree, P p) {
      if (tree == null) {
        return null;
      }
      if (tree.getKind().name().equals("SWITCH_EXPRESSION")) {
        // Don't scan nested switch expressions.
        return null;
      } else if (tree.getKind().name().equals("YIELD")) {
        ExpressionTree value = TreeUtils.yieldTreeGetValue(tree);
        return visitSwitchResultExpression(value, p);
      }
      return super.scan(tree, p);
    }

    @Override
    public R reduce(R r1, R r2) {
      return combineResults(r1, r2);
    }
  }

  /**
   * An implementation of {@link SwitchExpressionScanner} that uses functions passed to the
   * constructor for {@link #visitSwitchResultExpression(ExpressionTree, Object)} and {@link
   * #combineResults(Object, Object)}.
   *
   * @param <R1> the type result of {@link #visitSwitchResultExpression(ExpressionTree, Object)}
   * @param <P1> the type of the parameter to pass to {@link
   *     #visitSwitchResultExpression(ExpressionTree, Object)}
   */
  public static class FunctionalSwitchExpressionScanner<R1, P1>
      extends SwitchExpressionScanner<R1, P1> {

    /** The function to use for {@link #visitSwitchResultExpression(ExpressionTree, Object)}. */
    private final BiFunction<ExpressionTree, P1, R1> switchValueExpressionFunction;
    /** The function to use for {@link #visitSwitchResultExpression(ExpressionTree, Object)}. */
    private final BiFunction<@Nullable R1, @Nullable R1, R1> combineResultFunc;

    /**
     * Creates a {@link FunctionalSwitchExpressionScanner} that uses the given functions.
     *
     * @param switchValueExpressionFunc the function called on each switch result expression
     * @param combineResultFunc the function used to combine the result of multiple calls to {@code
     *     switchValueExpressionFunc}
     */
    public FunctionalSwitchExpressionScanner(
        BiFunction<ExpressionTree, P1, R1> switchValueExpressionFunc,
        BiFunction<@Nullable R1, @Nullable R1, R1> combineResultFunc) {
      this.switchValueExpressionFunction = switchValueExpressionFunc;
      this.combineResultFunc = combineResultFunc;
    }

    @Override
    protected R1 visitSwitchResultExpression(ExpressionTree resultExpressionTree, P1 p1) {
      return switchValueExpressionFunction.apply(resultExpressionTree, p1);
    }

    @Override
    protected R1 combineResults(@Nullable R1 r1, @Nullable R1 r2) {
      return combineResultFunc.apply(r1, r2);
    }
  }
}
