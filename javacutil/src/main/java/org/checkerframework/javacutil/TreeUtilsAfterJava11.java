package org.checkerframework.javacutil;

import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
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
  public static class CaseUtils {

    /** Don't use. */
    private CaseUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /** The {@code CaseTree.getExpressions} method for Java 12 and higher; null otherwise. */
    private static @Nullable Method GET_EXPRESSIONS = null;

    /** The {@code CaseTree.getBody} method for Java 12 and higher; null otherwise. */
    private static @Nullable Method GET_BODY = null;

    /** The {@code CaseTree.getKind()} method for Java 12 and higher; null otherwise. */
    private static @Nullable Method GET_KIND = null;

    /** The {@code CaseTree.getLabels} method for Java 21 and higher; null otherwise. */
    private static @Nullable Method GET_LABELS = null;

    /** The {@code CaseTree.getGuard} method for Java 21 and higher; null otherwise. */
    private static @Nullable Method GET_GUARD = null;

    /**
     * Returns true if this is a case rule (as opposed to a case statement).
     *
     * @param caseTree a case tree
     * @return true if {@code caseTree} is a case rule
     */
    public static boolean isCaseRule(CaseTree caseTree) {
      if (sourceVersionNumber < 12) {
        return false;
      }

      if (GET_KIND == null) {
        GET_KIND = getMethod(CaseTree.class, "getCaseKind");
      }
      Enum<?> kind = (Enum<?>) invokeNonNullResult(GET_KIND, caseTree);
      return kind.name().contentEquals("RULE");
    }

    /**
     * Returns the body of the case statement if it is of the form {@code case <expression> ->
     * <expression>}. This method should only be called if {@link CaseTree#getStatements()} returns
     * null.
     *
     * @param caseTree the case expression to get the body from
     * @return the body of the case tree
     */
    public static @Nullable Tree getBody(CaseTree caseTree) {
      assertVersionAtLeast(12);
      if (GET_BODY == null) {
        GET_BODY = getMethod(CaseTree.class, "getBody");
      }
      return (Tree) invoke(GET_BODY, caseTree);
    }

    /**
     * Returns true if this is the default case for a switch statement or expression. (Also, returns
     * true if {@code caseTree} is {@code case null, default:}.)
     *
     * @param caseTree a case tree
     * @return true if {@code caseTree} is the default case for a switch statement or expression
     */
    public static boolean isDefaultCaseTree(CaseTree caseTree) {
      if (sourceVersionNumber >= 21) {
        for (Tree label : getLabels(caseTree, true)) {
          if (isDefaultCaseLabelTree(label)) {
            return true;
          }
        }
        return false;
      } else {
        return getExpressions(caseTree).isEmpty();
      }
    }

    /**
     * Returns true if {@code tree} is a {@code DefaultCaseLabelTree}.
     *
     * @param tree a tree to check
     * @return true if {@code tree} is a {@code DefaultCaseLabelTree}
     */
    public static boolean isDefaultCaseLabelTree(Tree tree) {
      return tree.getKind().name().contentEquals("DEFAULT_CASE_LABEL");
    }

    /**
     * Get the list of labels from a case expression. For {@code default}, this is empty. For {@code
     * case null, default}, the list contains {@code null}. Otherwise, in JDK 11 and earlier, this
     * is a list of a single expression tree. In JDK 12+, the list may have multiple expression
     * trees. In JDK 21+, the list might contain a single pattern tree.
     *
     * @param caseTree the case expression to get the labels from
     * @return the list of case labels in the case
     */
    public static List<? extends Tree> getLabels(CaseTree caseTree) {
      return getLabels(caseTree, false);
    }

    /**
     * Get the list of labels from a case expression.
     *
     * <p>For JDKs before 21, if {@code caseTree} is the default case, then the returned list is
     * empty.
     *
     * <p>For 21+ JDK, if {@code useDefaultCaseLabelTree} is false, then if {@code caseTree} is the
     * default case or {@code case null, default}, then the returned list is empty. If {@code
     * useDefaultCaseLabelTree} is true, then if {@code caseTree} is the default case the returned
     * contains just a {@code DefaultCaseLabelTree}. If {@code useDefaultCaseLabelTree} is false,
     * then if {@code caseTree} is {@code case null, default} the returned list is a {@code
     * DefaultCaseLabelTree} and the expression tree for {@code null}.
     *
     * <p>Otherwise, in JDK 11 and earlier, this is a list of a single expression tree. In JDK 12+,
     * the list may have multiple expression trees. In JDK 21+, the list might contain a single
     * pattern tree.
     *
     * @param caseTree the case expression to get the labels from
     * @param useDefaultCaseLabelTree weather the result should contain a {@code
     *     DefaultCaseLabelTree}.
     * @return the list of case labels in the case
     */
    private static List<? extends Tree> getLabels(
        CaseTree caseTree, boolean useDefaultCaseLabelTree) {
      if (sourceVersionNumber >= 21) {
        if (GET_LABELS == null) {
          GET_LABELS = getMethod(CaseTree.class, "getLabels");
        }
        @SuppressWarnings("unchecked")
        List<? extends Tree> caseLabelTrees =
            (List<? extends Tree>) invokeNonNullResult(GET_LABELS, caseTree);
        List<Tree> labels = new ArrayList<>();
        for (Tree caseLabel : caseLabelTrees) {
          if (isDefaultCaseLabelTree(caseLabel)) {
            if (useDefaultCaseLabelTree) {
              labels.add(caseLabel);
            }
          } else if (ConstantCaseLabelUtils.isConstantCaseLabelTree(caseLabel)) {
            labels.add(ConstantCaseLabelUtils.getConstantExpression(caseLabel));
          } else if (PatternCaseLabelUtils.isPatternCaseLabelTree(caseLabel)) {
            labels.add(PatternCaseLabelUtils.getPattern(caseLabel));
          }
        }
        return labels;
      }
      return getExpressions(caseTree);
    }

    /**
     * Get the list of expressions from a case expression. For the default case, this is empty.
     * Otherwise, in JDK 11 and earlier, this is a singleton list. In JDK 12 onwards, there can be
     * multiple expressions per case.
     *
     * @param caseTree the case expression to get the expressions from
     * @return the list of expressions in the case
     */
    @SuppressWarnings("unchecked")
    public static List<? extends ExpressionTree> getExpressions(CaseTree caseTree) {
      if (sourceVersionNumber >= 12) {
        if (GET_EXPRESSIONS == null) {
          GET_EXPRESSIONS = getMethod(CaseTree.class, "getExpressions");
        }
        return (List<? extends ExpressionTree>) invokeNonNullResult(GET_EXPRESSIONS, caseTree);
      }
      @SuppressWarnings("deprecation") // getExpression is deprecated in Java 21
      ExpressionTree expression = caseTree.getExpression();
      if (expression == null) {
        return Collections.emptyList();
      }
      return Collections.singletonList(expression);
    }

    /**
     * Returns the guard, the expression after {@code when}, of {@code caseTree}. Wrapper around
     * {@code CaseTree#getGuard} that can be called on any version of Java.
     *
     * @param caseTree the case tree
     * @return the guard on the case tree or null if one does not exist
     */
    public static @Nullable ExpressionTree getGuard(CaseTree caseTree) {
      if (sourceVersionNumber < 21) {
        return null;
      }
      if (GET_GUARD == null) {
        GET_GUARD = getMethod(CaseTree.class, "getGuard");
      }
      return (ExpressionTree) invoke(GET_GUARD, caseTree);
    }
  }

  /** Utility methods for accessing {@code ConstantCaseLabelTree} methods. */
  public static class ConstantCaseLabelUtils {

    /** Don't use. */
    private ConstantCaseLabelUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /**
     * The {@code ConstantCaseLabelTree.getConstantExpression} method for Java 21 and higher; null
     * otherwise.
     */
    private static @Nullable Method GET_CONSTANT_EXPRESSION = null;

    /**
     * Returns true if {@code tree} is a {@code ConstantCaseLabelTree}.
     *
     * @param tree a tree to check
     * @return true if {@code tree} is a {@code ConstantCaseLabelTree}
     */
    public static boolean isConstantCaseLabelTree(Tree tree) {
      return tree.getKind().name().contentEquals("CONSTANT_CASE_LABEL");
    }

    /**
     * Wrapper around {@code ConstantCaseLabelTree#getConstantExpression}.
     *
     * @param constantCaseLabelTree a ConstantCaseLabelTree tree
     * @return the expression in the {@code constantCaseLabelTree}
     */
    public static ExpressionTree getConstantExpression(Tree constantCaseLabelTree) {
      assertVersionAtLeast(21);
      if (GET_CONSTANT_EXPRESSION == null) {
        Class<?> constantCaseLabelTreeClass =
            classForName("com.sun.source.tree.ConstantCaseLabelTree");
        GET_CONSTANT_EXPRESSION = getMethod(constantCaseLabelTreeClass, "getConstantExpression");
      }
      return (ExpressionTree) invokeNonNullResult(GET_CONSTANT_EXPRESSION, constantCaseLabelTree);
    }
  }

  /** Utility methods for accessing {@code DeconstructionPatternTree} methods. */
  public static class DeconstructionPatternUtils {

    /** Don't use. */
    private DeconstructionPatternUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /**
     * The {@code DeconstructionPatternTree.getDeconstructor} method for Java 21 and higher; null
     * otherwise.
     */
    private static @Nullable Method GET_DECONSTRUCTOR = null;

    /**
     * The {@code DeconstructionPatternTree.getNestedPatterns} method for Java 21 and higher; null
     * otherwise.
     */
    private static @Nullable Method GET_NESTED_PATTERNS = null;

    /**
     * Returns the deconstruction type of {@code tree}. Wrapper around {@code
     * DeconstructionPatternTree#getDeconstructor}.
     *
     * @param tree the DeconstructionPatternTree
     * @return the deconstructor of {@code DeconstructionPatternTree}
     */
    public static ExpressionTree getDeconstructor(Tree tree) {
      assertVersionAtLeast(21);
      if (GET_DECONSTRUCTOR == null) {
        Class<?> deconstructionPatternClass =
            classForName("com.sun.source.tree.DeconstructionPatternTree");
        GET_DECONSTRUCTOR = getMethod(deconstructionPatternClass, "getDeconstructor");
      }
      return (ExpressionTree) invokeNonNullResult(GET_DECONSTRUCTOR, tree);
    }

    /**
     * Wrapper around {@code DeconstructionPatternTree#getNestedPatterns}.
     *
     * @param tree the DeconstructionPatternTree
     * @return the nested patterns of {@code DeconstructionPatternTree}
     */
    @SuppressWarnings("unchecked")
    public static List<? extends Tree> getNestedPatterns(Tree tree) {
      assertVersionAtLeast(21);
      if (GET_NESTED_PATTERNS == null) {
        Class<?> deconstructionPatternClass =
            classForName("com.sun.source.tree.DeconstructionPatternTree");
        GET_NESTED_PATTERNS = getMethod(deconstructionPatternClass, "getNestedPatterns");
      }
      return (List<? extends Tree>) invokeNonNullResult(GET_NESTED_PATTERNS, tree);
    }
  }

  /** Utility methods for accessing {@code PatternCaseLabelTree} methods. */
  public static class PatternCaseLabelUtils {

    /** Don't use. */
    private PatternCaseLabelUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /** The PatternCaseLabelTree.getPattern method for Java 21 and higher; null otherwise. */
    private static @Nullable Method GET_PATTERN = null;

    /**
     * Returns whether {@code tree} is a {@code PatternCaseLabelTree}.
     *
     * @param tree a tree to check
     * @return true if {@code tree} is a {@code PatternCaseLabelTree}
     */
    public static boolean isPatternCaseLabelTree(Tree tree) {
      return tree.getKind().name().contentEquals("PATTERN_CASE_LABEL");
    }

    /**
     * Wrapper around {@code PatternCaseLabelTree#getPattern}.
     *
     * @param patternCaseLabelTree a PatternCaseLabelTree tree
     * @return the {@code PatternTree} in the {@code patternCaseLabelTree}
     */
    public static Tree getPattern(Tree patternCaseLabelTree) {
      assertVersionAtLeast(21);
      if (GET_PATTERN == null) {
        Class<?> patternCaseLabelClass = classForName("com.sun.source.tree.PatternCaseLabelTree");
        GET_PATTERN = getMethod(patternCaseLabelClass, "getPattern");
      }
      return (Tree) invokeNonNullResult(GET_PATTERN, patternCaseLabelTree);
    }
  }

  /** Utility methods for accessing {@code SwitchExpressionTree} methods. */
  public static class SwitchExpressionUtils {

    /** Don't use. */
    private SwitchExpressionUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /**
     * The {@code SwitchExpressionTree.getExpression} method for Java 12 and higher; null otherwise.
     */
    private static @Nullable Method GET_EXPRESSION = null;

    /** The {@code SwitchExpressionTree.getCases} method for Java 12 and higher; null otherwise. */
    private static @Nullable Method GET_CASES = null;

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
     */
    @SuppressWarnings("unchecked")
    public static List<? extends CaseTree> getCases(Tree switchExpressionTree) {
      assertVersionAtLeast(12);
      if (GET_CASES == null) {
        Class<?> switchExpressionClass = classForName("com.sun.source.tree.SwitchExpressionTree");
        GET_CASES = getMethod(switchExpressionClass, "getCases");
      }
      return (List<? extends CaseTree>) invokeNonNullResult(GET_CASES, switchExpressionTree);
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
     */
    public static ExpressionTree getExpression(Tree switchExpressionTree) {
      assertVersionAtLeast(12);
      if (GET_EXPRESSION == null) {
        Class<?> switchExpressionClass = classForName("com.sun.source.tree.SwitchExpressionTree");
        GET_EXPRESSION = getMethod(switchExpressionClass, "getExpression");
      }
      return (ExpressionTree) invokeNonNullResult(GET_EXPRESSION, switchExpressionTree);
    }
  }

  /** Utility methods for accessing {@code YieldTree} methods. */
  public static class YieldUtils {

    /** Don't use. */
    private YieldUtils() {
      throw new AssertionError("Cannot be instantiated.");
    }

    /** The {@code YieldTree.getValue} method for Java 13 and higher; null otherwise. */
    private static @Nullable Method GET_VALUE = null;

    /**
     * Returns the value (expression) for {@code yieldTree}.
     *
     * @param yieldTree the yield tree
     * @return the value (expression) for {@code yieldTree}
     */
    public static ExpressionTree getValue(Tree yieldTree) {
      assertVersionAtLeast(13);
      if (GET_VALUE == null) {
        Class<?> yieldTreeClass = classForName("com.sun.source.tree.YieldTree");
        GET_VALUE = getMethod(yieldTreeClass, "getValue");
      }
      return (ExpressionTree) invokeNonNullResult(GET_VALUE, yieldTree);
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
