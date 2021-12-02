package org.checkerframework.javacutil;

import com.sun.source.tree.CaseTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;

public abstract class CompatibilityTreeVisitor<R, P> extends SimpleTreeVisitor<R, P> {

  /**
   * Use {@link org.checkerframework.javacutil.TreeUtils#caseTreeGetExpressions(CaseTree)} instead
   * of calling {@link CaseTree#getExpression()}. Starting in Java 12, case trees can have multiple
   * expressions.
   *
   * @param caseTree case tree
   * @param p parameter
   * @return the result of visiting the case tree.
   */
  @Override
  public R visitCase(CaseTree caseTree, P p) {
    return super.visitCase(caseTree, p);
  }

  /**
   * Visit a BindingPatternTree.
   *
   * @param bindingPatternTree a BindingPatternTree, typed as Tree to be backward-compatible
   * @param p parameter
   * @return the result of visiting the binding pattern tree
   */
  public abstract R visitBindingPattern17(Tree bindingPatternTree, P p);

  /**
   * Visit a SwitchExpressionTree
   *
   * @param switchExpressionTree a SwitchExpressionTree, typed as Tree to be backward-compatible
   * @param p parameter
   * @return the result of visiting the switch expression tree
   */
  public abstract R visitSwitchExpression17(Tree switchExpressionTree, P p);

  /**
   * Visit a YieldTree.
   *
   * @param yieldTree a YieldTree, typed as Tree to be backward-compatible
   * @param p parameter
   * @return the result of visiting the yield tree
   */
  public abstract R visitYield17(Tree yieldTree, P p);

  /**
   * The default action for this visitor. This is inherited from SimpleTreeVisitor, but is only
   * called for those methods which do not have an override of the visitXXX method in this class.
   * Ultimately, those are the methods added post Java 11, such as for switch-expressions.
   *
   * @param tree the Javac tree
   * @param p the parameter
   * @return nothing
   */
  @Override
  protected R defaultAction(Tree tree, P p) {
    // Features added between JDK 12 and JDK 17 inclusive.
    // Must use String comparison to support compiling on JDK 11 and earlier:
    switch (tree.getKind().name()) {
      case "BINDING_PATTERN":
        return visitBindingPattern17(tree, p);
      case "SWITCH_EXPRESSION":
        return visitSwitchExpression17(tree, p);
      case "YIELD":
        return visitYield17(tree, p);
    }

    return super.defaultAction(tree, p);
  }
}
