package org.checkerframework.framework.test.junit;

import static org.checkerframework.framework.util.typeinference8.UninitializedInstance.uninitialized;

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
   * <p>{@code BoundSet.saveBounds} uses the copy constructor to snapshot the bound set before
   * {@code Resolution.resolveSmallestSet} attempts resolution without capture, and {@code
   * BoundSet.restore} copies the snapshot back into the bound set when that attempt fails. A copy
   * constructor that dropped the two annotation-failure fields would lose any qualifier violation
   * that had been recorded before the snapshot was taken, so {@code
   * InferenceResult.inferenceFailed()} could report success with an empty error message even though
   * a qualifier relationship had been violated.
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
   * Tests that {@link BoundSet#restore} undoes, in place, the changes that were made to the bound
   * set after {@link BoundSet#saveBounds} created the snapshot.
   *
   * <p>{@code Resolution} hands the same bound set to every step of inference, so restoring must
   * side-effect that bound set. If restoring instead returned the snapshot as a new bound set, then
   * a client that holds a reference to the original one -- such as {@code
   * InvocationTypeInference.getB4} -- would keep observing the state of the failed attempt.
   */
  @Test
  public void restoreUndoesChangesInPlace() {
    BoundSet boundSet = new BoundSet(uninitializedContext());
    BoundSet snapshot = boundSet.saveBounds();

    boundSet.addFalse();
    boundSet.setUncheckedConversion(true);
    boundSet.annoInferenceFailed = true;
    boundSet.errorMsg = "@Tainted String <: @Untainted String";

    boundSet.restore(snapshot);

    Assert.assertFalse(boundSet.containsFalse());
    Assert.assertFalse(boundSet.isUncheckedConversion());
    Assert.assertFalse(boundSet.annoInferenceFailed);
    Assert.assertEquals("", boundSet.errorMsg);
  }

  /**
   * Tests that {@link BoundSet#restore} discards exactly the state that was recorded after {@link
   * BoundSet#saveBounds} created the snapshot, and keeps the state that was recorded before it.
   *
   * <p>This is the situation that arises in {@code Resolution.resolveSmallestSet}. Incorporation
   * has already recorded a qualifier violation in the bound set by the time resolution starts, and
   * then the attempt at resolution without capture records a second one before it fails. Only the
   * second one is undone; the caller -- {@code InvocationTypeInference.getB4}, which reads {@code
   * annoInferenceFailed} and {@code errorMsg} off the bound set it passed to {@code
   * Resolution.resolve} -- must see the first one and must not see the second one.
   */
  @Test
  public void restoreDiscardsOnlyPostSnapshotState() {
    String beforeSnapshot = "@Tainted MyNode <: @Untainted Node<@Tainted MyNode>";
    BoundSet boundSet = new BoundSet(uninitializedContext());
    boundSet.annoInferenceFailed = true;
    boundSet.errorMsg = beforeSnapshot;

    BoundSet snapshot = boundSet.saveBounds();

    // The failed attempt at resolution without capture.
    boundSet.errorMsg +=
        System.lineSeparator() + "@Tainted Object <: @Untainted Tag<@Tainted Object>";
    boundSet.addFalse();
    boundSet.setUncheckedConversion(true);

    // The snapshot is unaffected by the failed attempt.
    Assert.assertEquals(beforeSnapshot, snapshot.errorMsg);
    Assert.assertFalse(snapshot.containsFalse());
    Assert.assertFalse(snapshot.isUncheckedConversion());

    boundSet.restore(snapshot);

    Assert.assertTrue(boundSet.annoInferenceFailed);
    Assert.assertEquals(beforeSnapshot, boundSet.errorMsg);
    Assert.assertFalse(boundSet.containsFalse());
    Assert.assertFalse(boundSet.isUncheckedConversion());
  }

  /**
   * Tests that a snapshot keeps its own copy of the state, so that a snapshot can be restored even
   * after {@link BoundSet#saveBounds} has taken a later snapshot of the same bound set.
   *
   * <p>{@code Resolution.resolveSmallestSet} takes a snapshot before each attempt at resolution
   * without capture, and reducing the constraints of one such attempt can start inference for a
   * nested expression, which resolves variables of its own and therefore takes a second snapshot of
   * the very same bound set.
   */
  @Test
  public void restoreOfEarlierSnapshotIsUnaffectedByLaterSaveBounds() {
    String beforeOuterSnapshot = "@Tainted MyNode <: @Untainted Node<@Tainted MyNode>";
    String beforeInnerSnapshot = "@Tainted Object <: @Untainted Tag<@Tainted Object>";
    BoundSet boundSet = new BoundSet(uninitializedContext());
    boundSet.annoInferenceFailed = true;
    boundSet.errorMsg = beforeOuterSnapshot;

    BoundSet outerSnapshot = boundSet.saveBounds();

    boundSet.errorMsg = beforeInnerSnapshot;
    boundSet.setUncheckedConversion(true);

    BoundSet innerSnapshot = boundSet.saveBounds();

    boundSet.addFalse();
    boundSet.errorMsg = "@Tainted String <: @Untainted String";

    // Restoring the inner snapshot undoes only what happened after it was taken.
    boundSet.restore(innerSnapshot);
    Assert.assertEquals(beforeInnerSnapshot, boundSet.errorMsg);
    Assert.assertTrue(boundSet.isUncheckedConversion());
    Assert.assertFalse(boundSet.containsFalse());

    // Restoring the outer snapshot undoes everything that happened after it was taken, including
    // the state that was current when the inner snapshot was taken.
    boundSet.restore(outerSnapshot);
    Assert.assertEquals(beforeOuterSnapshot, boundSet.errorMsg);
    Assert.assertFalse(boundSet.isUncheckedConversion());
    Assert.assertFalse(boundSet.containsFalse());
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
    public ConstantConstraint copy() {
      // A ConstantConstraint is immutable, so it is its own copy.
      return this;
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
}
