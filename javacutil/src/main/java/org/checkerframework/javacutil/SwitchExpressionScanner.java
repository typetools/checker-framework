package org.checkerframework.javacutil;

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreeScanner;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Scan the given switch expression and calls {@link #visitSwitchValueExpression(ExpressionTree,
 * Object)} on each expression that is a possible value of the switch expression. {@link
 * #combineResults(Object, Object)} is called to combine the results of visiting a switch value
 * expression.
 *
 * @param <R> result of {@link #visitSwitchValueExpression(ExpressionTree, Object)}
 * @param <P> parameter to pass to {@link #visitSwitchValueExpression(ExpressionTree, Object)}
 */
public abstract class SwitchExpressionScanner<R, P> extends TreeScanner<R, P> {

  /**
   * This method is called for each value expression of the switch expression passed in {@link
   * #visitSwitchValueExpressions(Tree, Object)}.
   *
   * @param valueTree a tree that is a possible value of the switch expression
   * @param p a parameter
   * @return the result of visiting the value expression
   */
  protected abstract R visitSwitchValueExpression(ExpressionTree valueTree, P p);

  /**
   * This method combines the result of two calls to {@link
   * #visitSwitchValueExpression(ExpressionTree, Object)} or {@code null} and the result of one call
   * to {@link #visitSwitchValueExpression(ExpressionTree, Object)}.
   *
   * @param r1 a possibly null result returned by {@link #visitSwitchValueExpression(ExpressionTree,
   *     Object)}
   * @param r2 a possibly null result returned by {@link #visitSwitchValueExpression(ExpressionTree,
   *     Object)}
   * @return the combination of {@code r1} and {@code r2}
   */
  protected abstract R combineResults(@Nullable R r1, @Nullable R r2);

  /**
   * Scan the given switch expression and calls {@link #visitSwitchValueExpression(ExpressionTree,
   * Object)} on each expression that is a possible value of the switch expression. {@link
   * #combineResults(Object, Object)} is called to combine the results of visiting a switch value
   * expression.
   *
   * @param switchExpression a switch expression tree
   * @param p the parameter to pass to {@link #visitSwitchValueExpression(ExpressionTree, Object)}
   * @return the result of calling {@link #visitSwitchValueExpression(ExpressionTree, Object)} on
   *     each value expression of {@code switchExpression} and combining the results using {@link
   *     #combineResults(Object, Object)}
   */
  public R visitSwitchValueExpressions(Tree switchExpression, P p) {
    assert switchExpression.getKind().name().equals("SWITCH_EXPRESSION");
    List<? extends CaseTree> caseTrees = TreeUtils.switchExpressionTreeGetCases(switchExpression);
    R result = null;
    for (CaseTree caseTree : caseTrees) {
      if (caseTree.getStatements() != null) {
        result = combineResults(result, scanner.scan(caseTree.getStatements(), p));
      } else {
        Tree body = TreeUtils.caseTreeGetBody(caseTree);
        if (body.getKind() == Kind.BLOCK) {
          result = combineResults(result, scanner.scan(((BlockTree) body).getStatements(), p));
        } else if (body.getKind() != Kind.THROW) {
          ExpressionTree expressionTree = (ExpressionTree) body;
          result = combineResults(result, visitSwitchValueExpression(expressionTree, p));
        }
      }
    }
    return result;
  }

  /**
   * A scanner that visits all the yield trees in a given tree and calls {@link
   * #visitSwitchValueExpression(ExpressionTree, Object)} on the expression in the yield trees. It
   * does not descend into switch expressions.
   */
  protected TreeScanner<R, P> scanner =
      new TreeScanner<R, P>() {
        @Override
        public R scan(Tree tree, P p) {
          if (tree == null) {
            return null;
          }
          if (tree.getKind().name().equals("SWITCH_EXPRESSION")) {
            // Don't scan nested switch expressions.
            return null;
          } else if (tree.getKind().name().equals("YIELD")) {
            ExpressionTree value = TreeUtils.yieldTreeGetValue(tree);
            return visitSwitchValueExpression(value, p);
          }
          return super.scan(tree, p);
        }

        @Override
        public R reduce(R r1, R r2) {
          return combineResults(r1, r2);
        }
      };
}
