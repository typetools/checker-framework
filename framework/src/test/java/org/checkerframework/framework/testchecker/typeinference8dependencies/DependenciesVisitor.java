package org.checkerframework.framework.testchecker.typeinference8dependencies;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.util.typeinference8.InvocationTypeInference;
import org.checkerframework.framework.util.typeinference8.types.Dependencies;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;

/**
 * Tests {@link Dependencies}, in particular that {@code Dependencies.get} tolerates a variable that
 * is not a key of the underlying map.
 *
 * <p>JLS 18.4 says that an inference variable depends on the resolution of itself, so the
 * dependencies of a variable about which nothing has been recorded are that variable alone. Before
 * the fix, {@code Dependencies.get} threw a {@code NullPointerException} for such a variable.
 * {@code Resolution.getSmallestDependencySet} and {@code ConstraintSet.getClosedSubset} both call
 * {@code Dependencies.get} with variables that the bound set that created the {@code Dependencies}
 * does not necessarily contain.
 *
 * <p>The tests are driven by the declarations in {@code
 * framework/tests/typeinference8dependencies/Typeinference8Dependencies.java}.
 */
public class DependenciesVisitor extends BaseTypeVisitor<DependenciesAnnotatedTypeFactory> {

  /** The simple name of the class in the test input that holds the declarations used here. */
  static final String TEST_CLASS_NAME = "Typeinference8Dependencies";

  /** True if the tests have been run. */
  static boolean testsRan = false;

  /**
   * Creates a {@code DependenciesVisitor}.
   *
   * @param checker the checker
   */
  public DependenciesVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  protected DependenciesAnnotatedTypeFactory createTypeFactory() {
    return new DependenciesAnnotatedTypeFactory(checker);
  }

  @Override
  public void processClassTree(ClassTree classTree) {
    super.processClassTree(classTree);
    if (!classTree.getSimpleName().contentEquals(TEST_CLASS_NAME)) {
      return;
    }

    // The two inference variables, for the type variables Z1 and Z2 of Holder.
    Iterator<Variable> variables = createVariables(classTree).iterator();
    Variable alpha = variables.next();
    Variable beta = variables.next();

    testsRan = true;

    List<String> failures = new ArrayList<>();
    testUnknownVariable(alpha, failures);
    testResultIsMutable(alpha, failures);
    testUnknownVariablesInList(alpha, beta, failures);
    testKnownVariable(alpha, beta, failures);

    if (!failures.isEmpty()) {
      throw new AssertionError(
          "Dependencies.get does not handle an unknown variable:"
              + System.lineSeparator()
              + String.join(System.lineSeparator(), failures));
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // The tests
  //

  /**
   * The dependencies of a variable that is not in the map are that variable alone.
   *
   * @param alpha a variable
   * @param failures the list to which to add a description of a failure
   */
  private void testUnknownVariable(Variable alpha, List<String> failures) {
    Dependencies dependencies = new Dependencies();
    Set<Variable> actual = get(dependencies, alpha, "an empty Dependencies", failures);
    if (actual != null && !actual.equals(Collections.singleton(alpha))) {
      failures.add(
          "get("
              + alpha
              + ") on an empty Dependencies returned "
              + actual
              + " rather than just the variable itself");
    }
  }

  /**
   * The result of {@code Dependencies.get} must be mutable, because {@code
   * Resolution.getSmallestDependencySet} removes the resolved variables from it.
   *
   * @param alpha a variable
   * @param failures the list to which to add a description of a failure
   */
  private void testResultIsMutable(Variable alpha, List<String> failures) {
    Dependencies dependencies = new Dependencies();
    Set<Variable> actual = get(dependencies, alpha, "an empty Dependencies", failures);
    if (actual == null) {
      return;
    }
    try {
      actual.removeAll(Collections.singletonList(alpha));
    } catch (Throwable t) {
      failures.add(
          "removing from the result of get("
              + alpha
              + ") on an empty Dependencies threw "
              + t.getClass().getName()
              + ": "
              + t);
    }
  }

  /**
   * The list overload of {@code Dependencies.get} also tolerates variables that are not in the map.
   *
   * @param alpha a variable
   * @param beta a different variable
   * @param failures the list to which to add a description of a failure
   */
  private void testUnknownVariablesInList(Variable alpha, Variable beta, List<String> failures) {
    Dependencies dependencies = new Dependencies();
    Set<Variable> actual;
    try {
      actual = dependencies.get(Arrays.asList(alpha, beta));
    } catch (Throwable t) {
      failures.add(
          "get(List.of("
              + alpha
              + ", "
              + beta
              + ")) on an empty Dependencies threw "
              + t.getClass().getName()
              + ": "
              + t);
      return;
    }
    Set<Variable> expected = new LinkedHashSet<>(Arrays.asList(alpha, beta));
    if (!actual.equals(expected)) {
      failures.add(
          "get(List.of("
              + alpha
              + ", "
              + beta
              + ")) on an empty Dependencies returned "
              + actual
              + " rather than "
              + expected);
    }
  }

  /**
   * A variable that is in the map is unaffected: its dependencies are the ones that were added.
   *
   * @param alpha a variable
   * @param beta a different variable
   * @param failures the list to which to add a description of a failure
   */
  private void testKnownVariable(Variable alpha, Variable beta, List<String> failures) {
    Dependencies dependencies = new Dependencies();
    dependencies.putOrAdd(alpha, beta);
    Set<Variable> actual =
        get(dependencies, alpha, "a Dependencies in which it is a key", failures);
    if (actual != null && !actual.equals(Collections.singleton(beta))) {
      failures.add(
          "get(" + alpha + ") returned " + actual + " rather than the recorded dependency " + beta);
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Helper methods
  //

  /**
   * Returns {@code dependencies.get(alpha)}, or null after adding to {@code failures} if the call
   * throws.
   *
   * @param dependencies the dependencies to query
   * @param alpha the variable to query
   * @param description a description of {@code dependencies}
   * @param failures the list to which to add a description of a failure
   * @return {@code dependencies.get(alpha)}, or null if the call threw
   */
  private @Nullable Set<Variable> get(
      Dependencies dependencies, Variable alpha, String description, List<String> failures) {
    try {
      return dependencies.get(alpha);
    } catch (Throwable t) {
      failures.add(
          "get(" + alpha + ") on " + description + " threw " + t.getClass().getName() + ": " + t);
      return null;
    }
  }

  /**
   * Creates the inference variables that the tests use: one for each type parameter of the class
   * {@code Holder} of the test input.
   *
   * @param classTree the class tree of the test input
   * @return two distinct inference variables
   */
  private Collection<Variable> createVariables(ClassTree classTree) {
    TreePath classPath = atypeFactory.getPath(classTree);
    // The InvocationTypeInference is needed only because Java8InferenceContext requires one.
    InvocationTypeInference inference = new InvocationTypeInference(atypeFactory, classPath);
    Java8InferenceContext context = new Java8InferenceContext(atypeFactory, classPath, inference);
    // `Holder<String, String>`; its type parameters Z1 and Z2 become the inference variables.
    ProperType holderType = new ProperType(field(classTree, "holder"), context);
    LambdaExpressionTree thetaKey =
        (LambdaExpressionTree) field(classTree, "thetaKeyLambda").getInitializer();
    Theta theta = context.inferenceTypeFactory.createThetaForLambda(thetaKey, holderType);
    if (theta.size() != 2) {
      throw new AssertionError(
          "Test input is wrong: expected 2 inference variables, found " + theta.values());
    }
    return theta.values();
  }

  /**
   * Returns the field of {@code classTree} with the given name.
   *
   * @param classTree a class tree
   * @param name the name of a field of {@code classTree}
   * @return the field of {@code classTree} with the given name
   */
  private static VariableTree field(ClassTree classTree, String name) {
    for (Tree member : classTree.getMembers()) {
      if (member instanceof VariableTree variableTree
          && variableTree.getName().contentEquals(name)) {
        return variableTree;
      }
    }
    throw new AssertionError(
        "Test input is wrong: no field " + name + " in " + classTree.getSimpleName());
  }
}
