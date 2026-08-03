package org.checkerframework.framework.testchecker.typeinference8;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.typeinference8.InvocationTypeInference;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.constraint.Expression;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult.ReductionResultPair;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.InferenceType;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Resolution;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Tests that the invariants of package {@code org.checkerframework.framework.util.typeinference8}
 * are enforced even when Java assertions are disabled.
 *
 * <p>Each invariant used to be expressed as an {@code assert} statement. Because assertions are
 * disabled by default, violating one was silently ignored in a released build. Each invariant is
 * now either a {@link BugInCF} (for an internal invariant) or a reduction to the {@code false}
 * bound (for a condition that the JLS says makes a constraint reduce to false). Each test below
 * violates one invariant and checks for the corresponding failure; before the fix, each test
 * instead observed an {@code AssertionError} (or, with assertions disabled, no failure at all).
 *
 * <p>The tests are driven by the declarations in {@code
 * framework/tests/typeinference8invariant/Typeinference8Invariants.java}.
 *
 * <p>Not every {@code assert} that was replaced is tested here. {@code
 * InvocationTypeInference.createB2}, {@code InvocationTypeInference.createB2MethodRef}, {@code
 * BoundSet.incorporateToFixedPoint}, and {@code VariableBounds.getConstraintsFromParameterized}
 * compute the value that they check from their arguments, and no argument makes the check fail:
 * respectively, {@code ConstraintSet.reduceOneStep} throws {@code FalseBoundException} before the
 * check is reached; incorporation of a well-formed bound set terminates; and two parameterized
 * supertypes of the same generic class always have the same number of type arguments.
 */
public class Typeinference8InvariantVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {

  /** The simple name of the class in the test input that holds the declarations used here. */
  private static final String TEST_CLASS_NAME = "Typeinference8Invariants";

  /**
   * Creates a {@code Typeinference8InvariantVisitor}.
   *
   * @param checker the checker
   */
  public Typeinference8InvariantVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  public void processClassTree(ClassTree classTree) {
    super.processClassTree(classTree);
    if (!classTree.getSimpleName().contentEquals(TEST_CLASS_NAME)) {
      return;
    }

    List<String> failures = new ArrayList<>();
    testResolveVariable(classTree, failures);
    testResolveCollection(classTree, failures);
    testResolveWithCapture(classTree, failures);
    testInferenceTypeMismatchedKinds(classTree, failures);
    testLambdaWithWrongNumberOfParameters(classTree, failures);
    testWildcardLambdaWithWrongNumberOfParameters(classTree, failures);

    if (!failures.isEmpty()) {
      throw new AssertionError(
          "Type inference invariants are not enforced:"
              + System.lineSeparator()
              + String.join(System.lineSeparator(), failures));
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // The tests
  //

  /**
   * {@code Resolution.resolve(Variable, BoundSet, Java8InferenceContext)} must fail if the bound
   * set contains false.
   *
   * @param classTree the class tree of the test input
   * @param failures the list to which to add a description of a failure
   */
  private void testResolveVariable(ClassTree classTree, List<String> failures) {
    Fixture fixture = new Fixture(classTree);
    BoundSet boundSet = fixture.falseBoundSet();
    expectBugInCF(
        failures,
        "Resolution.resolve(Variable, BoundSet, Java8InferenceContext) with a false bound set",
        () -> Resolution.resolve(fixture.alpha, boundSet, fixture.context));
  }

  /**
   * {@code Resolution.resolve(Collection, BoundSet, Java8InferenceContext)} must fail if the bound
   * set contains false.
   *
   * @param classTree the class tree of the test input
   * @param failures the list to which to add a description of a failure
   */
  private void testResolveCollection(ClassTree classTree, List<String> failures) {
    Fixture fixture = new Fixture(classTree);
    BoundSet boundSet = fixture.falseBoundSet();
    List<Variable> as = new ArrayList<>(Collections.singletonList(fixture.alpha));
    expectBugInCF(
        failures,
        "Resolution.resolve(Collection, BoundSet, Java8InferenceContext) with a false bound set",
        () -> Resolution.resolve(as, boundSet, fixture.context));
  }

  /**
   * {@code Resolution.resolveWithCapture} must fail if the bound set contains false. That method is
   * private, so this test calls it reflectively.
   *
   * @param classTree the class tree of the test input
   * @param failures the list to which to add a description of a failure
   */
  private void testResolveWithCapture(ClassTree classTree, List<String> failures) {
    Fixture fixture = new Fixture(classTree);
    BoundSet boundSet = fixture.falseBoundSet();
    Method resolveWithCapture;
    try {
      resolveWithCapture =
          Resolution.class.getDeclaredMethod(
              "resolveWithCapture",
              java.util.Set.class,
              BoundSet.class,
              Java8InferenceContext.class);
      resolveWithCapture.setAccessible(true);
    } catch (ReflectiveOperationException | RuntimeException e) {
      failures.add("Could not find Resolution.resolveWithCapture: " + e);
      return;
    }
    expectBugInCF(
        failures,
        "Resolution.resolveWithCapture with a false bound set",
        () -> {
          try {
            resolveWithCapture.invoke(
                null, Collections.singleton(fixture.alpha), boundSet, fixture.context);
          } catch (InvocationTargetException e) {
            throw asUnchecked(e.getCause());
          } catch (IllegalAccessException e) {
            throw new AssertionError(e);
          }
        });
  }

  /**
   * The {@code InferenceType} constructor must fail if the annotated type and the Java type have
   * different type kinds.
   *
   * @param classTree the class tree of the test input
   * @param failures the list to which to add a description of a failure
   */
  private void testInferenceTypeMismatchedKinds(ClassTree classTree, List<String> failures) {
    Fixture fixture = new Fixture(classTree);
    VariableTree listField = fixture.holderField("listField");
    AnnotatedTypeMirror listOfZ = atypeFactory.getAnnotatedType(listField);

    // Sanity check: `listOfZ` mentions the inference variable, so that the InferenceType
    // constructor (rather than the ProperType constructor) is used.
    AbstractType wellFormed =
        InferenceType.create(listOfZ, TreeUtils.typeOf(listField), fixture.theta, fixture.context);
    if (!(wellFormed instanceof InferenceType)) {
      failures.add(
          "Test setup is wrong: expected an InferenceType for List<Z>, but found "
              + wellFormed.getClass().getSimpleName());
      return;
    }

    TypeMirror arrayOfString =
        fixture
            .context
            .env
            .getTypeUtils()
            .getArrayType(
                fixture.context.env.getElementUtils().getTypeElement("java.lang.String").asType());
    expectBugInCF(
        failures,
        "InferenceType.create with a DECLARED annotated type and an ARRAY Java type",
        () -> InferenceType.create(listOfZ, arrayOfString, fixture.theta, fixture.context));
  }

  /**
   * JLS 18.2.1: if the number of lambda parameters differs from the number of parameter types of
   * the function type, the constraint reduces to false.
   *
   * @param classTree the class tree of the test input
   * @param failures the list to which to add a description of a failure
   */
  private void testLambdaWithWrongNumberOfParameters(ClassTree classTree, List<String> failures) {
    Fixture fixture = new Fixture(classTree);
    // `Supplier<alpha>`, whose function type has no parameters.
    AbstractType target = fixture.inferenceTypeOfHolderField("supplierField", failures);
    if (target == null) {
      return;
    }
    if (target.isWildcardParameterizedType()) {
      failures.add("Test setup is wrong: expected Supplier<alpha> not to have a wildcard.");
      return;
    }
    // An explicitly typed lambda with one parameter.
    LambdaExpressionTree lambda = fixture.lambdaField("oneParameterLambda");
    expectFalseBoundSet(
        failures,
        "<(String s) -> s --> Supplier<alpha>>",
        () -> new Expression("test", lambda, target).reduce(fixture.context));
  }

  /**
   * JLS 18.5.3: if the number of lambda parameters differs from the number of parameter types of
   * the function type of the wildcard-parameterized target type, no valid parameterization exists,
   * and so (JLS 18.2.1) the constraint reduces to false.
   *
   * @param classTree the class tree of the test input
   * @param failures the list to which to add a description of a failure
   */
  private void testWildcardLambdaWithWrongNumberOfParameters(
      ClassTree classTree, List<String> failures) {
    Fixture fixture = new Fixture(classTree);
    // `BiFunction<?, ?, alpha>`, whose function type has two parameters.
    AbstractType target = fixture.inferenceTypeOfHolderField("biFunctionField", failures);
    if (target == null) {
      return;
    }
    if (!target.isWildcardParameterizedType()) {
      failures.add("Test setup is wrong: expected BiFunction<?, ?, alpha> to have a wildcard.");
      return;
    }
    // An explicitly typed lambda with three parameters.
    LambdaExpressionTree lambda = fixture.lambdaField("threeParameterLambda");
    expectFalseBoundSet(
        failures,
        "<(String a, String b, String c) -> a --> BiFunction<?, ?, alpha>>",
        () -> new Expression("test", lambda, target).reduce(fixture.context));
  }

  // ///////////////////////////////////////////////////////////////////////////
  // Helper methods
  //

  /**
   * Runs {@code body} and adds a description to {@code failures} unless it throws {@link BugInCF}.
   *
   * @param failures the list to which to add a description of a failure
   * @param description a description of what {@code body} does
   * @param body the code that should throw {@link BugInCF}
   */
  private void expectBugInCF(List<String> failures, String description, Runnable body) {
    try {
      body.run();
    } catch (BugInCF e) {
      return;
    } catch (Throwable t) {
      failures.add(
          description + ": expected BugInCF, but " + t.getClass().getName() + " was thrown: " + t);
      return;
    }
    failures.add(description + ": expected BugInCF, but nothing was thrown");
  }

  /**
   * Runs {@code body} and adds a description to {@code failures} unless it returns a {@link
   * ReductionResultPair} whose bound set contains false.
   *
   * @param failures the list to which to add a description of a failure
   * @param description a description of the constraint that {@code body} reduces
   * @param body the code that should reduce to false
   */
  private void expectFalseBoundSet(
      List<String> failures,
      String description,
      java.util.function.Supplier<ReductionResult> body) {
    ReductionResult result;
    try {
      result = body.get();
    } catch (Throwable t) {
      failures.add(
          description
              + ": expected a reduction to false, but "
              + t.getClass().getName()
              + " was thrown: "
              + t);
      return;
    }
    if (!(result instanceof ReductionResultPair pair)) {
      failures.add(
          description + ": expected a ReductionResultPair, but the result was " + result + ".");
    } else if (!pair.boundSet().containsFalse()) {
      failures.add(description + ": expected the resulting bound set to contain false.");
    }
  }

  /**
   * Returns {@code t}, as an unchecked exception.
   *
   * @param t a throwable
   * @return {@code t}, as an unchecked exception
   */
  private static RuntimeException asUnchecked(Throwable t) {
    if (t instanceof RuntimeException runtimeException) {
      return runtimeException;
    }
    if (t instanceof Error error) {
      throw error;
    }
    return new RuntimeException(t);
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

  /**
   * Returns the class declared in {@code classTree} with the given name.
   *
   * @param classTree a class tree
   * @param name the name of a class declared in {@code classTree}
   * @return the class declared in {@code classTree} with the given name
   */
  private static ClassTree nestedClass(ClassTree classTree, String name) {
    for (Tree member : classTree.getMembers()) {
      if (member instanceof ClassTree nested && nested.getSimpleName().contentEquals(name)) {
        return nested;
      }
    }
    throw new AssertionError(
        "Test input is wrong: no class " + name + " in " + classTree.getSimpleName());
  }

  /**
   * The declarations and inference objects that a single test uses. Each test creates its own
   * fixture, because {@code InferenceFactory.createThetaForLambda} caches one {@code Theta} per
   * lambda tree per context.
   */
  private final class Fixture {

    /** The class tree of the test input. */
    private final ClassTree classTree;

    /** The nested class that declares types that mention a type variable. */
    private final ClassTree holderClass;

    /** The inference context. */
    private final Java8InferenceContext context;

    /** A map from the type variable {@code Z} of {@code Holder} to an inference variable. */
    private final Theta theta;

    /** The inference variable that {@link #theta} maps {@code Z} to. */
    private final Variable alpha;

    /**
     * Creates a fixture.
     *
     * @param classTree the class tree of the test input
     */
    Fixture(ClassTree classTree) {
      this.classTree = classTree;
      this.holderClass = nestedClass(classTree, "Holder");
      TreePath classPath = atypeFactory.getPath(classTree);
      InvocationTypeInference inference = new InvocationTypeInference(atypeFactory, classPath);
      this.context = new Java8InferenceContext(atypeFactory, classPath, inference);
      // `Holder<String>`; its type parameter Z becomes the inference variable.
      ProperType holderType = new ProperType(field(classTree, "holder"), context);
      this.theta =
          context.inferenceTypeFactory.createThetaForLambda(
              lambdaField("thetaKeyLambda"), holderType);
      this.alpha = theta.values().iterator().next();
    }

    /**
     * Returns a bound set that contains {@link #alpha} and the false bound.
     *
     * @return a bound set that contains {@link #alpha} and the false bound
     */
    BoundSet falseBoundSet() {
      BoundSet boundSet = BoundSet.initialBounds(theta, context);
      boundSet.addFalse();
      return boundSet;
    }

    /**
     * Returns the initializer of the given field of the test input, which must be a lambda.
     *
     * @param name the name of a field of the test input whose initializer is a lambda
     * @return the initializer of the given field of the test input
     */
    LambdaExpressionTree lambdaField(String name) {
      ExpressionTree initializer = field(classTree, name).getInitializer();
      return (LambdaExpressionTree) initializer;
    }

    /**
     * Returns the given field of {@code Holder}.
     *
     * @param name the name of a field of {@code Holder}
     * @return the given field of {@code Holder}
     */
    VariableTree holderField(String name) {
      return field(holderClass, name);
    }

    /**
     * Returns the type of the given field of {@code Holder}, with {@code Z} replaced by the
     * inference variable. Returns null, after adding to {@code failures}, if the result is not an
     * {@link InferenceType}.
     *
     * @param name the name of a field of {@code Holder}
     * @param failures the list to which to add a description of a failure
     * @return the type of the given field of {@code Holder}, or null
     */
    AbstractType inferenceTypeOfHolderField(String name, List<String> failures) {
      VariableTree fieldTree = holderField(name);
      AbstractType result =
          InferenceType.create(
              atypeFactory.getAnnotatedType(fieldTree),
              TreeUtils.typeOf(fieldTree),
              theta,
              context);
      if (result.isProper()) {
        failures.add(
            "Test setup is wrong: the type of Holder."
                + name
                + " should mention the inference variable, but it is the proper type "
                + result
                + ".");
        return null;
      }
      return result;
    }
  }
}
