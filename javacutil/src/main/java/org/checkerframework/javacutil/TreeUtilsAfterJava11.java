package org.checkerframework.javacutil;

import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CaseTree.CaseKind;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.YieldTree;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import javax.lang.model.SourceVersion;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetName;
import org.checkerframework.dataflow.qual.Pure;

/**
 * This class contains utility methods for reflectively accessing Tree classes and methods that were
 * added after Java 11.
 */
public class TreeUtilsAfterJava11 {

  /** Don't use. */
  private TreeUtilsAfterJava11() {
    throw new AssertionError("Cannot be instantiated.");
  }

  /** The latest source version supported by this compiler. */
  private static final int sourceVersionNumber =
      Integer.parseInt(SourceVersion.latest().toString().substring("RELEASE_".length()));

  /** Utility methods for accessing {@code BindingPatternTree} methods. */
  public static class BindingPatternUtils {

    /** Don't use. */
    private BindingPatternUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /** The {@code BindingPatternTree.getVariable} method for Java 16 and higher; null otherwise. */
    private static @Nullable Method GET_VARIABLE = null;

    /**
     * Returns the binding variable of {@code bindingPatternTree}.
     *
     * @param bindingPatternTree the BindingPatternTree whose binding variable is returned
     * @return the binding variable of {@code bindingPatternTree}
     */
    public static VariableTree getVariable(Tree bindingPatternTree) {
      assertVersionAtLeast(16);
      if (GET_VARIABLE == null) {
        Class<?> bindingPatternClass = classForName("com.sun.source.tree.BindingPatternTree");
        GET_VARIABLE = getMethod(bindingPatternClass, "getVariable");
      }
      return (VariableTree) invokeNonNullResult(GET_VARIABLE, bindingPatternTree);
    }
  }

  /** Utility methods for accessing {@code CaseTree} methods. */
  @Deprecated(forRemoval = true, since = "2026-03-25")
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
    @Deprecated(forRemoval = true, since = "2026-03-25")
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
    @Deprecated(forRemoval = true, since = "2026-03-25")
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
    @Deprecated(forRemoval = true, since = "2026-03-25")
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
    @Deprecated(forRemoval = true, since = "2026-03-25")
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
    @Deprecated(forRemoval = true, since = "2026-03-25")
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
     * @deprecated {@link TreeUtilsAfterJava17.CaseUtils#getExpressions(CaseTree)}
     */
    @SuppressWarnings("unchecked")
    @Deprecated(forRemoval = true, since = "2026-03-25")
    public static List<? extends ExpressionTree> getExpressions(CaseTree caseTree) {
      return TreeUtilsAfterJava17.CaseUtils.getExpressions(caseTree);
    }

    /**
     * Returns the guard, the expression after {@code when}, of {@code caseTree}. Wrapper around
     * {@code CaseTree#getGuard} that can be called on any version of Java.
     *
     * @param caseTree the case tree
     * @return the guard on the case tree or null if one does not exist
     * @deprecated {@link TreeUtilsAfterJava17.CaseUtils#getGuard(CaseTree)}
     */
    @Deprecated(forRemoval = true, since = "2026-03-25")
    public static @Nullable ExpressionTree getGuard(CaseTree caseTree) {
      return TreeUtilsAfterJava17.CaseUtils.getGuard(caseTree);
    }
  }

  /**
   * Utility methods for accessing {@code ConstantCaseLabelTree} methods.
   *
   * @deprecated use {@link TreeUtilsAfterJava17.ConstantCaseLabelUtils}
   */
  @Deprecated(forRemoval = true, since = "2026-03-25")
  public static class ConstantCaseLabelUtils {
    /** Don't use. */
    private ConstantCaseLabelUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }
  }

  /**
   * Utility methods for accessing {@code DeconstructionPatternTree} methods.
   *
   * @deprecated use {@link TreeUtilsAfterJava17.DeconstructionPatternUtils}
   */
  @Deprecated(forRemoval = true, since = "2026-03-25")
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
    @Deprecated(forRemoval = true, since = "2026-03-25")
    public static ExpressionTree getDeconstructor(Tree tree) {
      return TreeUtilsAfterJava17.DeconstructionPatternUtils.getDeconstructor(tree);
    }

    /**
     * Wrapper around {@code DeconstructionPatternTree#getNestedPatterns}.
     *
     * @param tree the DeconstructionPatternTree
     * @return the nested patterns of {@code DeconstructionPatternTree}
     * @deprecated {@link TreeUtilsAfterJava17.DeconstructionPatternUtils#getNestedPatterns(Tree)}
     *     (Tree)}
     */
    @Deprecated(forRemoval = true, since = "2026-03-25")
    public static List<? extends Tree> getNestedPatterns(Tree tree) {
      return TreeUtilsAfterJava17.DeconstructionPatternUtils.getNestedPatterns(tree);
    }
  }

  /**
   * Utility methods for accessing {@code PatternCaseLabelTree} methods.
   *
   * @deprecated use {@link TreeUtilsAfterJava17.PatternCaseLabelUtils}
   */
  @Deprecated(forRemoval = true, since = "2026-03-25")
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
    @Deprecated(forRemoval = true, since = "2026-03-25")
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
    @Deprecated(forRemoval = true, since = "2026-03-25")
    public static Tree getPattern(Tree patternCaseLabelTree) {
      return TreeUtilsAfterJava17.PatternCaseLabelUtils.getPattern(patternCaseLabelTree);
    }
  }

  /**
   * Utility methods for accessing {@code SwitchExpressionTree} methods.
   *
   * @deprecated use {@link SwitchExpressionTree}
   */
  @Deprecated(forRemoval = true, since = "2026-03-25")
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
    @Deprecated(forRemoval = true, since = "2026-03-25")
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
    @Deprecated(forRemoval = true, since = "2026-03-25")
    public static ExpressionTree getExpression(Tree switchExpressionTree) {
      return ((SwitchExpressionTree) switchExpressionTree).getExpression();
    }
  }

  /** Utility methods for accessing {@code YieldTree} methods. */
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
    @Deprecated(forRemoval = true, since = "2026-03-25")
    public static ExpressionTree getValue(Tree yieldTree) {
      return ((YieldTree) yieldTree).getValue();
    }
  }

  /** Utility methods for accessing {@code JCVariableDecl} methods. */
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
    @Deprecated(forRemoval = true, since = "2026-03-25")
    @Pure
    public static boolean declaredUsingVar(JCVariableDecl variableTree) {
      return variableTree.declaredUsingVar();
    }
  }

  /** Utility methods for accessing {@code InstanceOfTree} methods. */
  public static class InstanceOfUtils {

    /** Don't use. */
    private InstanceOfUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /** The {@code InstanceOfTree.getPattern} method for Java 16 and higher; null otherwise. */
    private static @Nullable Method GET_PATTERN = null;

    /**
     * Returns the pattern of {@code instanceOfTree} tree. Returns null if the instanceof does not
     * have a pattern, including if the JDK version does not support instance-of patterns.
     *
     * @param instanceOfTree the {@link InstanceOfTree} whose pattern is returned
     * @return the {@code PatternTree} of {@code instanceOfTree} or null if it doesn't exist
     */
    @Pure
    public static @Nullable Tree getPattern(InstanceOfTree instanceOfTree) {
      if (sourceVersionNumber < 16) {
        return null;
      }
      if (GET_PATTERN == null) {
        GET_PATTERN = getMethod(InstanceOfTree.class, "getPattern");
      }
      return (Tree) invoke(GET_PATTERN, instanceOfTree);
    }
  }

  /**
   * Asserts that the latest source version is at least {@code version}.
   *
   * @param version version to check
   * @throws BugInCF if the latest version is smaller than {@code version}
   */
  private static void assertVersionAtLeast(int version) {
    if (sourceVersionNumber < version) {
      throw new BugInCF(
          "Method call requires at least Java version %s, but the current version is %s",
          version, sourceVersionNumber);
    }
  }

  /**
   * Reflectively invokes {@code method} with {@code receiver}; rethrowing any exceptions as {@code
   * BugInCF} exceptions. If the results is {@code null} a {@code BugInCF} is thrown.
   *
   * @param method a method
   * @param receiver the receiver for the method
   * @return the result of invoking {@code method} on {@code receiver}
   */
  private static Object invokeNonNullResult(Method method, Tree receiver) {
    Object result = invoke(method, receiver);
    if (result != null) {
      return result;
    }
    throw new BugInCF(
        "Expected nonnull result for method invocation: %s for tree: %s",
        method.getName(), receiver);
  }

  /**
   * Reflectively invokes {@code method} with {@code receiver}; rethrowing any exceptions as {@code
   * BugInCF} exceptions.
   *
   * @param method a method
   * @param receiver the receiver for the method
   * @return the result of invoking {@code method} on {@code receiver}
   */
  private static @Nullable Object invoke(Method method, Tree receiver) {
    try {
      return method.invoke(receiver);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new BugInCF(
          e, "Reflection failed for method: %s for tree: %s", method.getName(), receiver);
    }
  }

  /**
   * Returns the {@link Method} object for the method with name {@code name} in class {@code clazz}.
   * Rethrowing any exceptions as {@code BugInCF} exceptions.
   *
   * @param clazz a class
   * @param name a method name
   * @return the {@link Method} object for the method with name {@code name} in class {@code clazz}
   */
  private static Method getMethod(Class<?> clazz, String name) {
    try {
      return clazz.getMethod(name);
    } catch (NoSuchMethodException e) {
      throw new BugInCF("Method %s not found in class %s", name, clazz);
    }
  }

  /**
   * Returns the class named {@code name}. Rethrows any exceptions as {@code BugInCF} exceptions.
   *
   * @param name a class name
   * @return the class named {@code name}
   */
  private static Class<?> classForName(@ClassGetName String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      throw new BugInCF("Class not found " + name);
    }
  }
}
