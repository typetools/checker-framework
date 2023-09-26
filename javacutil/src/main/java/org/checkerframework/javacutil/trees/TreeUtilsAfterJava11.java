package org.checkerframework.javacutil.trees;

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
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * This class contains util methods for reflective accessing Tree classes and methods that were
 * added after Java 11.
 */
public class TreeUtilsAfterJava11 {

  /** The latest source version supported by this compiler. */
  private static final int sourceVersionNumber =
      Integer.parseInt(SourceVersion.latest().toString().substring("RELEASE_".length()));

  /** Utility methods for accessing {@code BindingPatternTree} methods. */
  public static class BindingPatternUtils {

    /** The {@code BindingPatternTree.getVariable} method for Java 16 and higher; null otherwise. */
    private static @Nullable Method GETVARIABLE = null;

    /**
     * Returns the binding variable of {@code bindingPatternTree}.
     *
     * @param bindingPatternTree the BindingPatternTree whose binding variable is returned
     * @return the binding variable of {@code bindingPatternTree}
     */
    public static VariableTree getVariable(Tree bindingPatternTree) {
      assertVersionAtLeast(16);
      if (GETVARIABLE == null) {
        Class<?> bindingPatternClass = classForName("com.sun.source.tree.BindingPatternTree");
        GETVARIABLE = getMethod(bindingPatternClass, "getVariable");
      }
      return (VariableTree) invokeNonNullResult(GETVARIABLE, bindingPatternTree);
    }
  }

  /** Utility methods for accessing {@code CaseTree} methods. */
  public static class CaseUtils {

    /** The {@code CaseTree.getExpressions} method for Java 12 and higher; null otherwise. */
    private static @Nullable Method GETEXPRESSIONS;

    /** The {@code CaseTree.getBody} method for Java 12 and higher; null otherwise. */
    private static @Nullable Method GETBODY;

    /** The {@code CaseTree.getKind()} method for Java 12 and higher; null otherwise. */
    private static @Nullable Method GETKIND;

    /** The {@code CaseTree.getLabels} method for Java 21 and higher; null otherwise. */
    private static @Nullable Method GETLABELS;

    /** The {@code CaseTree.getGuard} method for Java 21 and higher; null otherwise. */
    private static @Nullable Method GETGUARD;

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

      if (GETKIND == null) {
        GETKIND = getMethod(CaseTree.class, "getCaseKind");
      }
      Enum<?> kind = (Enum<?>) invokeNonNullResult(GETKIND, caseTree);
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
      if (GETBODY == null) {
        GETBODY = getMethod(CaseTree.class, "getBody");
      }
      return (Tree) invoke(GETBODY, caseTree);
    }

    /**
     * Get the list of labels from a case expression. For {@code default}, this is empty. Otherwise,
     * in JDK 11 and earlier, this is a singleton list of expression trees. In JDK 12, this is a
     * list of expression trees. In JDK 21+, this is a list of expression and pattern trees.
     *
     * @param caseTree the case expression to get the labels from
     * @return the list of case labels in the case
     */
    public static List<? extends Tree> getLabels(CaseTree caseTree) {
      if (sourceVersionNumber >= 21) {
        if (GETLABELS == null) {
          GETLABELS = getMethod(CaseTree.class, "getLabels");
        }
        @SuppressWarnings("unchecked")
        List<? extends Tree> caseLabelTrees =
            (List<? extends Tree>) invokeNonNullResult(GETLABELS, caseTree);
        List<Tree> unWrappedLabels = new ArrayList<>();
        for (Tree caseLabel : caseLabelTrees) {
          if (TreeUtils.isDefaultCaseLabelTree(caseLabel)) {
            return Collections.emptyList();
          } else if (TreeUtils.isConstantCaseLabelTree(caseLabel)) {
            unWrappedLabels.add(ConstantCaseLabelUtils.getConstantExpression(caseLabel));
          } else if (TreeUtils.isPatternCaseLabelTree(caseLabel)) {
            unWrappedLabels.add(PatternCaseLabelUtils.getPattern(caseLabel));
          }
        }
        return unWrappedLabels;
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
        if (GETEXPRESSIONS == null) {
          GETEXPRESSIONS = getMethod(CaseTree.class, "getExpressions");
        }
        return (List<? extends ExpressionTree>) invokeNonNullResult(GETEXPRESSIONS, caseTree);
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
      if (GETGUARD == null) {
        GETGUARD = getMethod(CaseTree.class, "getGuard");
      }
      return (ExpressionTree) invokeNonNullResult(GETGUARD, caseTree);
    }
  }

  /** Utility methods for accessing {@code ConstantCaseLabelTree} methods. */
  public static class ConstantCaseLabelUtils {

    /**
     * The {@code ConstantCaseLabelTree.getConstantExpression} method for Java 21 and higher; null
     * otherwise.
     */
    private static @Nullable Method GETCONSTANTEXPRESSION;

    /**
     * Wrapper around {@code ConstantCaseLabelTree#getConstantExpression}.
     *
     * @param constantCaseLabelTree a ConstantCaseLabelTree tree
     * @return the expression in the {@code constantCaseLabelTree}
     */
    public static ExpressionTree getConstantExpression(Tree constantCaseLabelTree) {
      assertVersionAtLeast(21);
      if (GETCONSTANTEXPRESSION == null) {
        Class<?> constantCaseLabelTreeClass =
            classForName("com.sun.source.tree.ConstantCaseLabelTree");
        GETCONSTANTEXPRESSION = getMethod(constantCaseLabelTreeClass, "getConstantExpression");
      }
      return (ExpressionTree) invokeNonNullResult(GETCONSTANTEXPRESSION, constantCaseLabelTree);
    }
  }

  /** Utility methods for accessing {@code DeconstructionPatternTree} methods. */
  public static class DeconstructionPatternUtils {
    /**
     * The {@code DeconstructionPatternTree.getDeconstructor} method for Java 21 and higher; null
     * otherwise.
     */
    private static @Nullable Method GETDECONSTRUCTOR;

    /**
     * The {@code DeconstructionPatternTree.getNestedPatterns} method for Java 21 and higher; null
     * otherwise.
     */
    private static @Nullable Method GETNESTEDPATTERNS;

    /**
     * Returns the deconstruction type of {@code tree}. Wrapper around {@code
     * DeconstructionPatternTree#getDeconstructor}.
     *
     * @param tree the DeconstructionPatternTree
     * @return the deconstructor of {@code DeconstructionPatternTree}
     */
    public static ExpressionTree getDeconstructor(Tree tree) {
      assertVersionAtLeast(21);
      if (GETDECONSTRUCTOR == null) {
        Class<?> deconstructionPatternClass =
            classForName("com.sun.source.tree.DeconstructionPatternTree");
        GETDECONSTRUCTOR = getMethod(deconstructionPatternClass, "getDeconstructor");
      }
      return (ExpressionTree) invokeNonNullResult(GETDECONSTRUCTOR, tree);
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
      if (GETNESTEDPATTERNS == null) {
        Class<?> deconstructionPatternClass =
            classForName("com.sun.source.tree.DeconstructionPatternTree");
        GETNESTEDPATTERNS = getMethod(deconstructionPatternClass, "getNestedPatterns");
      }
      return (List<? extends Tree>) invokeNonNullResult(GETNESTEDPATTERNS, tree);
    }
  }

  /** Utility methods for accessing {@code PatternCaseLabelTree} methods. */
  public static class PatternCaseLabelUtils {

    /** The PatternCaseLabelTree.getPattern method for Java 21 and higher; null otherwise. */
    private static @Nullable Method GETPATTERN;

    /**
     * Wrapper around {@code PatternCaseLabelTree#getPattern}.
     *
     * @param patternCaseLabelTree a PatternCaseLabelTree tree
     * @return the {@code PatternTree} in the {@code patternCaseLabelTree}
     */
    public static Tree getPattern(Tree patternCaseLabelTree) {
      assertVersionAtLeast(21);
      if (GETPATTERN == null) {
        Class<?> patternCaseLabelClass = classForName("com.sun.source.tree.PatternCaseLabelTree");
        GETPATTERN = getMethod(patternCaseLabelClass, "getPattern");
      }
      return (Tree) invokeNonNullResult(GETPATTERN, patternCaseLabelTree);
    }
  }

  /** Utility methods for accessing {@code SwitchExpressionTree} methods. */
  public static class SwitchExpressionUtils {

    /**
     * The {@code SwitchExpressionTree.getExpression} method for Java 12 and higher; null otherwise.
     */
    private static @Nullable Method GETEXPRESSION;

    /** The {@code SwitchExpressionTree.getCases} method for Java 12 and higher; null otherwise. */
    private static @Nullable Method GETCASES;

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
      if (GETCASES == null) {
        Class<?> switchExpressionClass = classForName("com.sun.source.tree.SwitchExpressionTree");
        GETCASES = getMethod(switchExpressionClass, "getCases");
      }
      return (List<? extends CaseTree>) invokeNonNullResult(GETCASES, switchExpressionTree);
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
      if (GETEXPRESSION == null) {
        Class<?> switchExpressionClass = classForName("com.sun.source.tree.SwitchExpressionTree");
        GETEXPRESSION = getMethod(switchExpressionClass, "getExpression");
      }
      return (ExpressionTree) invokeNonNullResult(GETEXPRESSION, switchExpressionTree);
    }
  }

  /** Utility methods for accessing {@code YieldTree} methods. */
  public static class YieldUtils {

    /** The {@code YieldTree.getValue} method for Java 13 and higher; null otherwise. */
    private static @Nullable Method GETVALUE;

    /**
     * Returns the value (expression) for {@code yieldTree}.
     *
     * @param yieldTree the yield tree
     * @return the value (expression) for {@code yieldTree}
     */
    public static ExpressionTree getValue(Tree yieldTree) {
      assertVersionAtLeast(13);
      if (GETVALUE == null) {
        Class<?> yieldTreeClass = classForName("com.sun.source.tree.YieldTree");
        GETVALUE = getMethod(yieldTreeClass, "getValue");
      }
      return (ExpressionTree) invokeNonNullResult(GETVALUE, yieldTree);
    }
  }

  /** Utility methods for accessing {@code InstanceOfTree} methods. */
  public static class InstanceOfUtils {

    /** The {@code InstanceOfTree.getPattern} method for Java 16 and higher; null otherwise. */
    private static @Nullable Method GETPATTERN;

    /**
     * Returns the pattern of {@code instanceOfTree} tree. Returns null if the instanceof does not
     * have a pattern, including if the JDK version does not support instance-of patterns.
     *
     * @param instanceOfTree the {@link InstanceOfTree} whose pattern is returned
     * @return the {@code PatternTree} of {@code instanceOfTree} or null if it doesn't exist
     */
    public static @Nullable Tree getPattern(InstanceOfTree instanceOfTree) {
      if (sourceVersionNumber < 16) {
        return null;
      }
      if (GETPATTERN == null) {
        GETPATTERN = getMethod(InstanceOfTree.class, "getPattern");
      }
      return (Tree) invoke(GETPATTERN, instanceOfTree);
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
  private static Class<?> classForName(String name) {
    try {
      return Class.forName(name);
    } catch (ClassNotFoundException e) {
      throw new BugInCF("Class not found " + name);
    }
  }
}
