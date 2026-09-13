package org.checkerframework.framework.test.junit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.typeinference8.bound.BoundSet;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.junit.Assert;
import org.junit.Test;

/** Tests of {@link BoundSet}. */
public class BoundSetTest {

  /**
   * Tests that {@link BoundSet#BoundSet(BoundSet)} copies every field of the bound set, including
   * the two fields that record that inference failed because of a type qualifier: {@code
   * annoInferenceFailed} and {@code errorMsg}.
   *
   * <p>{@code Resolution.resolveSmallestSet} uses the copy constructor to snapshot the bound set
   * before attempting resolution without capture, and it discards the bound set in favor of the
   * snapshot when that attempt fails. A copy constructor that dropped the two annotation-failure
   * fields would lose any qualifier violation that had been recorded before the snapshot was taken,
   * so {@code InferenceResult.inferenceFailed()} could report success with an empty error message
   * even though a qualifier relationship had been violated.
   */
  @Test
  public void copyConstructorCopiesAllFields() {
    BoundSet original = new BoundSet(uninitializedContext());
    Assert.assertFalse(original.annoInferenceFailed);
    Assert.assertEquals("", original.errorMsg);
    Assert.assertFalse(original.containsFalse());
    Assert.assertFalse(original.isUncheckedConversion());

    original.annoInferenceFailed = true;
    original.errorMsg = "@Tainted String <: @Untainted String";
    original.addFalse();
    original.setUncheckedConversion(true);

    BoundSet copy = new BoundSet(original);

    Assert.assertTrue(copy.annoInferenceFailed);
    Assert.assertEquals("@Tainted String <: @Untainted String", copy.errorMsg);
    Assert.assertTrue(copy.containsFalse());
    Assert.assertTrue(copy.isUncheckedConversion());
  }

  /**
   * Tests that {@link BoundSet#incorporateToFixedPoint} tolerates a variable being added to the
   * bound set while the bound set's variables are being iterated over.
   *
   * <p>{@code incorporateToFixedPoint} iterates over the bound set's variables, reducing each
   * variable's constraints and merging the resulting bound set into the bound set being iterated
   * over. {@link BoundSet#merge} adds the merged bound set's variables to this bound set, so
   * iterating over the set itself throws {@link ConcurrentModificationException} when reduction
   * produces a variable that the bound set did not already contain.
   */
  @Test
  public void incorporationCanAddAVariable() {
    Java8InferenceContext context = uninitializedContext();
    Variable alpha = new TestVariable(context, 1);
    Variable beta = new TestVariable(context, 2);
    Variable gamma = new TestVariable(context, 3);
    // Reducing alpha's only constraint produces a bound set that contains gamma, which is not in
    // the bound set that is being incorporated to a fixed point.
    BoundSet reductionResult = BoundSet.initialBounds(thetaFor(gamma), context);
    alpha.getBounds().constraints.add(new ConstantConstraint(reductionResult));
    // Gamma has a constraint of its own, which is reduced only if gamma is processed after being
    // added to the bound set.
    gamma.getBounds().constraints.add(new ConstantConstraint(new BoundSet(context)));

    // The bound set contains beta as well as alpha, so that gamma is added to the bound set while
    // the iteration still has an element to yield.  A `LinkedHashSet` iterator does not notice an
    // element that is added after the iterator has yielded the set's last element.
    BoundSet boundSet = BoundSet.initialBounds(thetaFor(alpha, beta), context);
    boundSet.incorporateToFixedPoint(new BoundSet(context));

    Assert.assertTrue(alpha.getBounds().constraints.isEmpty());
    Assert.assertTrue(gamma.getBounds().constraints.isEmpty());
  }

  /** A variable that can be created without a running compilation. */
  private static class TestVariable extends Variable {

    /**
     * Creates a variable for testing.
     *
     * @param context the context
     * @param id an identification number, which is unique among the variables of one test
     */
    TestVariable(Java8InferenceContext context, int id) {
      super(uninitialized(AnnotatedTypeVariable.class), null, null, context, null, id);
    }

    // Variable's equals and hashCode use the Java type variable, which is null in a TestVariable.

    @Override
    public boolean equals(Object o) {
      return this == o;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @Override
    public String toString() {
      return "TestVariable " + id;
    }
  }

  /** A constraint that reduces to a fixed bound set. */
  private static class ConstantConstraint implements Constraint {

    /** The result of reducing this constraint. */
    private final BoundSet reductionResult;

    /**
     * Creates a constraint that reduces to {@code reductionResult}.
     *
     * @param reductionResult the result of reducing this constraint
     */
    ConstantConstraint(BoundSet reductionResult) {
      this.reductionResult = reductionResult;
    }

    @Override
    public Kind getKind() {
      return Kind.TYPE_EQUALITY;
    }

    @Override
    public ReductionResult reduce(Java8InferenceContext context) {
      return reductionResult;
    }
  }

  /**
   * Returns a {@link Theta} whose {@link Theta#values} are {@code variables}. The mapping is
   * created by overriding {@code values} rather than by calling {@link Theta#put}, because {@code
   * put} requires a type variable, which requires a running compilation. {@link
   * BoundSet#initialBounds} reads only {@code values}.
   *
   * @param variables the inference variables that the result maps to
   * @return a {@link Theta} whose values are {@code variables}
   */
  private static Theta thetaFor(Variable... variables) {
    List<Variable> values = List.of(variables);
    return new Theta() {
      @Override
      public Collection<Variable> values() {
        return values;
      }
    };
  }

  /**
   * Returns a {@link Java8InferenceContext} on which no constructor has run, so all its fields are
   * null. Creating a real context requires a running compilation, which is far more than this test
   * needs: {@link BoundSet}'s constructors only store the reference and check that it is non-null.
   *
   * @return a {@link Java8InferenceContext} whose fields are all null
   */
  private static Java8InferenceContext uninitializedContext() {
    return uninitialized(Java8InferenceContext.class);
  }

  /**
   * Returns an instance of {@code clazz} on which no constructor has run, so all its fields are
   * null or zero.
   *
   * <p>The instance is created the way deserialization creates one, via {@code
   * sun.reflect.ReflectionFactory}. That class is used reflectively so that compiling this file
   * does not produce a "internal proprietary API" warning, which {@code -Werror} would turn into an
   * error.
   *
   * @param <T> the type of the instance to create
   * @param clazz the class of the instance to create
   * @return an instance of {@code clazz} whose fields all have their default values
   */
  private static <T> T uninitialized(Class<T> clazz) {
    try {
      Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
      Object reflectionFactory =
          reflectionFactoryClass.getMethod("getReflectionFactory").invoke(null);
      Method newConstructorForSerialization =
          reflectionFactoryClass.getMethod(
              "newConstructorForSerialization", Class.class, Constructor.class);
      Constructor<?> constructor =
          (Constructor<?>)
              newConstructorForSerialization.invoke(
                  reflectionFactory, clazz, Object.class.getDeclaredConstructor());
      return clazz.cast(constructor.newInstance());
    } catch (ReflectiveOperationException e) {
      throw new AssertionError("Could not create a " + clazz.getSimpleName(), e);
    }
  }
}
