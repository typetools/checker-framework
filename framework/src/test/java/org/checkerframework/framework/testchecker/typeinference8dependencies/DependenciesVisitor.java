package org.checkerframework.framework.testchecker.typeinference8dependencies;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
 * <p>JLS 18.4 says that an inference variable depends on the resolution of itself, so {@code
 * Dependencies.get} always includes the queried variable in its result, and the dependencies of a
 * variable about which nothing has been recorded are that variable alone. Before the fix, {@code
 * Dependencies.get} threw a {@code NullPointerException} for such a variable. {@code
 * Resolution.getSmallestDependencySet} and {@code ConstraintSet.getClosedSubset} both call {@code
 * Dependencies.get} with variables that the bound set that created the {@code Dependencies} does
 * not necessarily contain.
 *
 * <p>Each test method throws an {@code AssertionError} on failure, so that the stack trace points
 * at the failing check.
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

    // The three inference variables, for the type variables Z1, Z2, and Z3 of Holder.
    List<Variable> variables = createVariables(classTree);
    Variable alpha = variables.get(0);
    Variable beta = variables.get(1);
    Variable gamma = variables.get(2);

    testsRan = true;

    testUnknownVariable(alpha);
    testResultIsMutable(alpha);
    testUnknownVariablesInList(alpha, beta);
    testMixedVariablesInList(alpha, beta, gamma);
    testKnownVariable(alpha, beta);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // The tests
  //

  /**
   * The dependencies of a variable that is not in the map are that variable alone.
   *
   * @param alpha a variable
   */
  private static void testUnknownVariable(Variable alpha) {
    Dependencies dependencies = new Dependencies();
    Set<Variable> actual = dependencies.get(alpha);
    if (!actual.equals(Collections.singleton(alpha))) {
      throw new AssertionError(
          "get("
              + alpha
              + ") on an empty Dependencies returned "
              + actual
              + " rather than just the variable itself");
    }
  }

  /**
   * The result of {@code Dependencies.get} must be mutable, because {@code
   * Resolution.getSmallestDependencySet} removes the resolved variables from it. This method throws
   * {@code UnsupportedOperationException} if the result is immutable.
   *
   * @param alpha a variable
   */
  private static void testResultIsMutable(Variable alpha) {
    Dependencies dependencies = new Dependencies();
    dependencies.get(alpha).removeAll(Collections.singletonList(alpha));
  }

  /**
   * The list overload of {@code Dependencies.get} also tolerates variables that are not in the map.
   *
   * @param alpha a variable
   * @param beta a different variable
   */
  private static void testUnknownVariablesInList(Variable alpha, Variable beta) {
    Dependencies dependencies = new Dependencies();
    Set<Variable> actual = dependencies.get(Arrays.asList(alpha, beta));
    Set<Variable> expected = new LinkedHashSet<>(Arrays.asList(alpha, beta));
    if (!actual.equals(expected)) {
      throw new AssertionError(
          "get(["
              + alpha
              + ", "
              + beta
              + "]) on an empty Dependencies returned "
              + actual
              + " rather than "
              + expected);
    }
  }

  /**
   * The list overload of {@code Dependencies.get} handles a list that mixes a variable that is in
   * the map with one that is not. {@code ConstraintSet.getClosedSubset} passes such lists.
   *
   * @param alpha a variable, which is in the map
   * @param beta a different variable, which is a recorded dependency of {@code alpha}
   * @param gamma a third variable, which is not in the map
   */
  private static void testMixedVariablesInList(Variable alpha, Variable beta, Variable gamma) {
    Dependencies dependencies = new Dependencies();
    dependencies.putOrAdd(alpha, beta);
    Set<Variable> actual = dependencies.get(Arrays.asList(alpha, gamma));
    Set<Variable> expected = new LinkedHashSet<>(Arrays.asList(alpha, beta, gamma));
    if (!actual.equals(expected)) {
      throw new AssertionError(
          "get([" + alpha + ", " + gamma + "]) returned " + actual + " rather than " + expected);
    }
  }

  /**
   * The dependencies of a variable that is in the map are the recorded ones plus the variable
   * itself.
   *
   * @param alpha a variable
   * @param beta a different variable
   */
  private static void testKnownVariable(Variable alpha, Variable beta) {
    Dependencies dependencies = new Dependencies();
    dependencies.putOrAdd(alpha, beta);
    Set<Variable> actual = dependencies.get(alpha);
    Set<Variable> expected = new LinkedHashSet<>(Arrays.asList(alpha, beta));
    if (!actual.equals(expected)) {
      throw new AssertionError(
          "get("
              + alpha
              + ") returned "
              + actual
              + " rather than "
              + expected
              + ", which is the recorded dependency plus the variable itself");
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Helper methods
  //

  /**
   * Creates the inference variables that the tests use: one for each type parameter of the class
   * {@code Holder} of the test input.
   *
   * @param classTree the class tree of the test input
   * @return three distinct inference variables
   */
  private List<Variable> createVariables(ClassTree classTree) {
    TreePath classPath = atypeFactory.getPath(classTree);
    // The InvocationTypeInference is needed only because Java8InferenceContext requires one.
    InvocationTypeInference inference = new InvocationTypeInference(atypeFactory, classPath);
    Java8InferenceContext context = new Java8InferenceContext(atypeFactory, classPath, inference);
    // `Holder<String, String, String>`; its type parameters Z1, Z2, and Z3 become the inference
    // variables.
    ProperType holderType = new ProperType(field(classTree, "holder"), context);
    LambdaExpressionTree thetaKey =
        (LambdaExpressionTree) field(classTree, "thetaKeyLambda").getInitializer();
    Theta theta = context.inferenceTypeFactory.createThetaForLambda(thetaKey, holderType);
    if (theta.size() != 3) {
      throw new AssertionError(
          "Test input is wrong: expected 3 inference variables, found " + theta.values());
    }
    return new ArrayList<>(theta.values());
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
