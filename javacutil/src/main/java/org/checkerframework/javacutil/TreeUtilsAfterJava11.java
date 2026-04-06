package org.checkerframework.javacutil;

import com.sun.source.tree.BindingPatternTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CaseTree.CaseKind;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.YieldTree;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

/**
 * This class contains utility methods for reflectively accessing Tree classes and methods that were
 * added after Java 11.
 */
@Deprecated(forRemoval = true, since = "4.0.0")
public class TreeUtilsAfterJava11 {

  /** Don't use. */
  private TreeUtilsAfterJava11() {
    throw new AssertionError("Cannot be instantiated.");
  }

  /** Utility methods for accessing {@code BindingPatternTree} methods. */
  @Deprecated(forRemoval = true, since = "4.0.0")
  public static class BindingPatternUtils {

    /** Don't use. */
    private BindingPatternUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /**
     * Returns the binding variable of {@code bindingPatternTree}.
     *
     * @param bindingPatternTree the BindingPatternTree whose binding variable is returned
     * @return the binding variable of {@code bindingPatternTree}
     * @deprecated Use {@link BindingPatternTree#getVariable()}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static VariableTree getVariable(Tree bindingPatternTree) {
      return ((BindingPatternTree) bindingPatternTree).getVariable();
    }
  }

  /** Utility methods for accessing {@code CaseTree} methods. */
  @Deprecated(forRemoval = true, since = "4.0.0")
  public static class CaseUtils {

    /** Don't use. */
    private CaseUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /**
     * Returns true if this is a case rule (as opposed to a case statement).
     *
     * @param caseTree a case tree
     * @return true if {@code caseTree} is a case rule
     * @deprecated use {@link CaseTree#getCaseKind()}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static boolean isCaseRule(CaseTree caseTree) {
      return caseTree.getCaseKind() == CaseKind.RULE;
    }

    /**
     * Returns the body of the case statement if it is of the form {@code case <expression> ->
     * <expression>}. This method should only be called if {@link CaseTree#getStatements()} returns
     * null.
     *
     * @param caseTree the case expression to get the body from
     * @return the body of the case tree
     * @deprecated use {@link CaseTree#getBody()}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static @Nullable Tree getBody(CaseTree caseTree) {
      return caseTree.getBody();
    }

    /**
     * Returns true if this is the default case for a switch statement or expression. (Also, returns
     * true if {@code caseTree} is {@code case null, default:}.)
     *
     * @param caseTree a case tree
     * @return true if {@code caseTree} is the default case for a switch statement or expression
     * @deprecated {@link TreeUtilsAfterJava17.CaseUtils#isDefaultCaseTree(CaseTree)}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static boolean isDefaultCaseTree(CaseTree caseTree) {
      return TreeUtilsAfterJava17.CaseUtils.isDefaultCaseTree(caseTree);
    }

    /**
     * Returns true if {@code tree} is a {@code DefaultCaseLabelTree}.
     *
     * @param tree a tree to check
     * @return true if {@code tree} is a {@code DefaultCaseLabelTree}
     * @deprecated {@link TreeUtilsAfterJava17.CaseUtils#isDefaultCaseLabelTree(Tree)}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static boolean isDefaultCaseLabelTree(Tree tree) {
      return TreeUtilsAfterJava17.CaseUtils.isDefaultCaseLabelTree(tree);
    }

    /**
     * Returns the list of labels from a case expression. For {@code default}, this is empty. For
     * {@code case null, default}, the list contains {@code null}. Otherwise, in JDK 11 and earlier,
     * this is a list of a single expression tree. In JDK 12+, the list may have multiple expression
     * trees. In JDK 21+, the list might contain a single pattern tree.
     *
     * @param caseTree the case expression to get the labels from
     * @return the list of case labels in the case
     * @deprecated {@link TreeUtilsAfterJava17.CaseUtils#getLabels(CaseTree)}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static List<? extends Tree> getLabels(CaseTree caseTree) {
      return TreeUtilsAfterJava17.CaseUtils.getLabels(caseTree);
    }

    /**
     * Returns the list of expressions from a case expression. For the default case, this is empty.
     * Otherwise, in JDK 11 and earlier, this is a singleton list. In JDK 12 onwards, there can be
     * multiple expressions per case.
     *
     * @param caseTree the case expression to get the expressions from
     * @return the list of expressions in the case
     * @deprecated {@link CaseTree#getExpressions()}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static List<? extends ExpressionTree> getExpressions(CaseTree caseTree) {
      return caseTree.getExpressions();
    }

    /**
     * Returns the guard, the expression after {@code when}, of {@code caseTree}. Wrapper around
     * {@code CaseTree#getGuard} that can be called on any version of Java.
     *
     * @param caseTree the case tree
     * @return the guard on the case tree or null if one does not exist
     * @deprecated {@link TreeUtilsAfterJava17.CaseUtils#getGuard(CaseTree)}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static @Nullable ExpressionTree getGuard(CaseTree caseTree) {
      return TreeUtilsAfterJava17.CaseUtils.getGuard(caseTree);
    }
  }

  /** Utility methods for accessing {@code ConstantCaseLabelTree} methods. */
  @Deprecated(forRemoval = true, since = "4.0.0")
  public static class ConstantCaseLabelUtils {
    /** Don't use. */
    private ConstantCaseLabelUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /**
     * Returns true if {@code tree} is a {@code ConstantCaseLabelTree}.
     *
     * @param tree a tree to check
     * @return true if {@code tree} is a {@code ConstantCaseLabelTree}
     * @deprecated use {@link
     *     TreeUtilsAfterJava17.ConstantCaseLabelUtils#isConstantCaseLabelTree(Tree)}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static boolean isConstantCaseLabelTree(Tree tree) {
      return TreeUtilsAfterJava17.ConstantCaseLabelUtils.isConstantCaseLabelTree(tree);
    }

    /**
     * Wrapper around {@code ConstantCaseLabelTree#getConstantExpression}.
     *
     * @param constantCaseLabelTree a ConstantCaseLabelTree tree
     * @return the expression in the {@code constantCaseLabelTree}
     * @deprecated use {@link
     *     TreeUtilsAfterJava17.ConstantCaseLabelUtils#getConstantExpression(Tree)}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static ExpressionTree getConstantExpression(Tree constantCaseLabelTree) {
      return TreeUtilsAfterJava17.ConstantCaseLabelUtils.getConstantExpression(
          constantCaseLabelTree);
    }
  }

  /** Utility methods for accessing {@code DeconstructionPatternTree} methods. */
  @Deprecated(forRemoval = true, since = "4.0.0")
  public static class DeconstructionPatternUtils {

    /** Don't use. */
    private DeconstructionPatternUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /**
     * Returns the deconstruction type of {@code tree}. Wrapper around {@code
     * DeconstructionPatternTree#getDeconstructor}.
     *
     * @param tree the DeconstructionPatternTree
     * @return the deconstructor of {@code DeconstructionPatternTree}
     * @deprecated {@link TreeUtilsAfterJava17.DeconstructionPatternUtils#getDeconstructor(Tree)}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static ExpressionTree getDeconstructor(Tree tree) {
      return TreeUtilsAfterJava17.DeconstructionPatternUtils.getDeconstructor(tree);
    }

    /**
     * Wrapper around {@code DeconstructionPatternTree#getNestedPatterns}.
     *
     * @param tree the DeconstructionPatternTree
     * @return the nested patterns of {@code DeconstructionPatternTree}
     * @deprecated {@link TreeUtilsAfterJava17.DeconstructionPatternUtils#getNestedPatterns(Tree)}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static List<? extends Tree> getNestedPatterns(Tree tree) {
      return TreeUtilsAfterJava17.DeconstructionPatternUtils.getNestedPatterns(tree);
    }
  }

  /** Utility methods for accessing {@code PatternCaseLabelTree} methods. */
  @Deprecated(forRemoval = true, since = "4.0.0")
  public static class PatternCaseLabelUtils {

    /** Don't use. */
    private PatternCaseLabelUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /**
     * Returns true if {@code tree} is a {@code PatternCaseLabelTree}.
     *
     * @param tree a tree to check
     * @return true if {@code tree} is a {@code PatternCaseLabelTree}
     * @deprecated use {@link
     *     TreeUtilsAfterJava17.PatternCaseLabelUtils#isPatternCaseLabelTree(Tree)}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static boolean isPatternCaseLabelTree(Tree tree) {
      return TreeUtilsAfterJava17.PatternCaseLabelUtils.isPatternCaseLabelTree(tree);
    }

    /**
     * Wrapper around {@code PatternCaseLabelTree#getPattern}.
     *
     * @param patternCaseLabelTree a PatternCaseLabelTree tree
     * @return the {@code PatternTree} in the {@code patternCaseLabelTree}
     * @deprecated use {@link TreeUtilsAfterJava17.PatternCaseLabelUtils#getPattern(Tree)}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static Tree getPattern(Tree patternCaseLabelTree) {
      return TreeUtilsAfterJava17.PatternCaseLabelUtils.getPattern(patternCaseLabelTree);
    }
  }

  /** Utility methods for accessing {@code SwitchExpressionTree} methods. */
  @Deprecated(forRemoval = true, since = "4.0.0")
  public static class SwitchExpressionUtils {

    /** Don't use. */
    private SwitchExpressionUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /**
     * Returns the cases of {@code switchExpressionTree}. For example
     *
     * <pre>
     *   switch ( <em>expression</em> ) {
     *     <em>cases</em>
     *   }
     * </pre>
     *
     * @param switchExpressionTree the switch expression whose cases are returned
     * @return the cases of {@code switchExpressionTree}
     * @deprecated {@link SwitchExpressionTree#getCases()}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static List<? extends CaseTree> getCases(Tree switchExpressionTree) {
      return ((SwitchExpressionTree) switchExpressionTree).getCases();
    }

    /**
     * Returns the selector expression of {@code switchExpressionTree}. For example
     *
     * <pre>
     *   switch ( <em>expression</em> ) { ... }
     * </pre>
     *
     * @param switchExpressionTree the switch expression whose selector expression is returned
     * @return the selector expression of {@code switchExpressionTree}
     * @deprecated {@link SwitchExpressionTree#getExpression()}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static ExpressionTree getExpression(Tree switchExpressionTree) {
      return ((SwitchExpressionTree) switchExpressionTree).getExpression();
    }
  }

  /** Utility methods for accessing {@code YieldTree} methods. */
  @Deprecated(forRemoval = true, since = "4.0.0")
  public static class YieldUtils {

    /** Don't use. */
    private YieldUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /**
     * Returns the value (expression) for {@code yieldTree}.
     *
     * @param yieldTree the yield tree
     * @return the value (expression) for {@code yieldTree}
     * @deprecated Use {@link YieldTree#getValue()}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    public static ExpressionTree getValue(Tree yieldTree) {
      return ((YieldTree) yieldTree).getValue();
    }
  }

  /** Utility methods for accessing {@code JCVariableDecl} methods. */
  @Deprecated(forRemoval = true, since = "4.0.0")
  public static class JCVariableDeclUtils {

    /** Don't use. */
    private JCVariableDeclUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /**
     * For Java 17+, returns true if {@code variableTree} was declared using {@code var}. Otherwise,
     * returns false.
     *
     * <p>Use {@link TreeUtils#isVariableTreeDeclaredUsingVar(VariableTree)} for a method that works
     * on all versions of java.
     *
     * @param variableTree a variable tree
     * @return true if {@code variableTree} was declared using {@code var} and using Java 17+
     * @deprecated Use {@link JCVariableDecl#declaredUsingVar()}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    @Pure
    public static boolean declaredUsingVar(JCVariableDecl variableTree) {
      return variableTree.declaredUsingVar();
    }
  }

  /** Utility methods for accessing {@code InstanceOfTree} methods. */
  @Deprecated(forRemoval = true, since = "4.0.0")
  public static class InstanceOfUtils {

    /** Don't use. */
    private InstanceOfUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /**
     * Returns the pattern of {@code instanceOfTree} tree. Returns null if the instanceof does not
     * have a pattern, including if the JDK version does not support instance-of patterns.
     *
     * @param instanceOfTree the {@link InstanceOfTree} whose pattern is returned
     * @return the {@code PatternTree} of {@code instanceOfTree} or null if it doesn't exist
     * @deprecated Use {@link InstanceOfTree#getPattern()}
     */
    @Deprecated(forRemoval = true, since = "4.0.0")
    @Pure
    public static @Nullable Tree getPattern(InstanceOfTree instanceOfTree) {
      return instanceOfTree.getPattern();
    }
  }
}
