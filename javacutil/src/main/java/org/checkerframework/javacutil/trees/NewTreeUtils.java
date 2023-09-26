package org.checkerframework.javacutil.trees;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import javax.lang.model.SourceVersion;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.BugInCF;

/**
 * This class contains util methods for reflective accessing Tree classes and methods that were
 * added after Java 8.
 */
public class NewTreeUtils {

  /** The latest source version supported by this compiler. */
  private static final int sourceVersionNumber =
      Integer.parseInt(SourceVersion.latest().toString().substring("RELEASE_".length()));

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
