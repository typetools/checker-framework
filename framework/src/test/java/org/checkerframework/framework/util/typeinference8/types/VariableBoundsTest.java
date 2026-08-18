package org.checkerframework.framework.util.typeinference8.types;

import static org.checkerframework.framework.util.typeinference8.UninitializedInstance.uninitialized;

import org.checkerframework.framework.util.typeinference8.constraint.Constraint;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.junit.Assert;
import org.junit.Test;

/** Tests of {@link VariableBounds#save} and {@link VariableBounds#restore}. */
public class VariableBoundsTest {

  /** Creates a new VariableBoundsTest. */
  public VariableBoundsTest() {}

  /** A constraint that is never reduced; only its identity matters. */
  private static class DummyConstraint implements Constraint {

    /** Creates a new DummyConstraint. */
    DummyConstraint() {}

    @Override
    public Kind getKind() {
      return Kind.SUBTYPE;
    }

    @Override
    public DummyConstraint copy() {
      // A DummyConstraint is immutable, so it is its own copy.
      return this;
    }

    @Override
    public ReductionResult reduce(Java8InferenceContext context) {
      throw new AssertionError("VariableBoundsTest never reduces a constraint.");
    }
  }

  /**
   * A constraint that reduction changes in place, as {@code Typing} does when {@code
   * ConstraintSet#applyInstantiations} replaces a variable by its instantiation.
   */
  private static class MutableConstraint implements Constraint {

    /** The part of this constraint that {@link #mutate} changes. */
    private String value;

    /**
     * Creates a constraint with the given value.
     *
     * @param value the value of the new constraint
     */
    MutableConstraint(String value) {
      this.value = value;
    }

    /**
     * Changes this constraint in place, as applying an instantiation to it does.
     *
     * @param newValue the new value of this constraint
     */
    void mutate(String newValue) {
      this.value = newValue;
    }

    @Override
    public Kind getKind() {
      return Kind.SUBTYPE;
    }

    @Override
    public MutableConstraint copy() {
      return new MutableConstraint(value);
    }

    @Override
    public ReductionResult reduce(Java8InferenceContext context) {
      throw new AssertionError("VariableBoundsTest never reduces a constraint.");
    }
  }

  /**
   * Tests that {@link VariableBounds#restore} discards the constraints and the throws bound that
   * were recorded after {@link VariableBounds#save}.
   *
   * <p>{@code Resolution.resolveWithoutCapture} adds an {@code EQUAL} bound for each variable it
   * resolves, and incorporating such a bound can enqueue a constraint in {@link
   * VariableBounds#constraints}. Those constraints are not always reduced before the attempt fails:
   * {@code BoundSet.incorporateToFixedPoint} returns immediately when the bound set contains false.
   * A constraint left over from the failed attempt was derived from bounds that restoration
   * discards, so restoration must discard the constraint too. Otherwise the {@code
   * BoundSet.reachFixedPoint} call at the end of {@code Resolution.resolveWithCapture} would reduce
   * it, which can put false back into the bound set and make {@code Resolution.resolve} throw
   * {@code BugInCF}.
   */
  @Test
  public void restoreDiscardsConstraintsFromFailedAttempt() {
    VariableBounds variableBounds = uninitializedVariableBounds();

    VariableBounds.Snapshot snapshot = variableBounds.save();

    // The failed attempt at resolution without capture.
    variableBounds.constraints.add(new DummyConstraint());
    variableBounds.setHasThrowsBound(true);

    variableBounds.restore(snapshot);

    Assert.assertTrue(variableBounds.constraints.isEmpty());
    Assert.assertFalse(variableBounds.hasThrowsBound());
  }

  /**
   * Tests that {@link VariableBounds#restore} keeps the constraints and the throws bound that were
   * recorded before {@link VariableBounds#save}, which are not part of the failed attempt.
   */
  @Test
  public void restoreKeepsStateFromBeforeTheSnapshot() {
    VariableBounds variableBounds = uninitializedVariableBounds();
    Constraint beforeSnapshot = new DummyConstraint();
    variableBounds.constraints.add(beforeSnapshot);
    variableBounds.setHasThrowsBound(true);

    VariableBounds.Snapshot snapshot = variableBounds.save();

    // The failed attempt at resolution without capture.
    variableBounds.constraints.add(new DummyConstraint());

    variableBounds.restore(snapshot);

    Assert.assertTrue(variableBounds.hasThrowsBound());
    Assert.assertSame(beforeSnapshot, variableBounds.constraints.pop());
    Assert.assertTrue(variableBounds.constraints.isEmpty());
  }

  /**
   * Tests that {@link VariableBounds#restore} undoes a change that the failed attempt made to a
   * constraint that was recorded before {@link VariableBounds#save}.
   *
   * <p>Incorporating a bound calls {@code VariableBounds#applyInstantiationsToBounds}, which
   * changes each of the constraints in place, replacing a variable by the instantiation that the
   * attempt chose. If a snapshot shared its constraints with the bounds, restoration would
   * reinstall a constraint that mentions an instantiation that restoration has just discarded, and
   * the {@code BoundSet.reachFixedPoint} call at the end of {@code Resolution.resolveWithCapture}
   * would reduce it.
   */
  @Test
  public void restoreUndoesChangesToConstraintsFromBeforeTheSnapshot() {
    VariableBounds variableBounds = uninitializedVariableBounds();
    MutableConstraint beforeSnapshot = new MutableConstraint("original");
    variableBounds.constraints.add(beforeSnapshot);

    VariableBounds.Snapshot snapshot = variableBounds.save();

    // The failed attempt at resolution without capture applies its instantiations to the
    // constraints, which changes them in place.
    beforeSnapshot.mutate("from the failed attempt");

    variableBounds.restore(snapshot);

    MutableConstraint restored = (MutableConstraint) variableBounds.constraints.pop();
    Assert.assertEquals("original", restored.value);
    Assert.assertTrue(variableBounds.constraints.isEmpty());

    // Restoration did not hand out the snapshot's own constraint, so the snapshot is still
    // restorable.
    variableBounds.constraints.add(restored);
    restored.mutate("from a later failed attempt");
    variableBounds.restore(snapshot);
    Assert.assertEquals("original", ((MutableConstraint) variableBounds.constraints.pop()).value);
    Assert.assertTrue(variableBounds.constraints.isEmpty());
  }

  /**
   * Tests that a snapshot keeps its own copy of the state, so that a snapshot can be restored even
   * after a later snapshot of the same bounds has been taken.
   *
   * <p>{@code Resolution.resolveSmallestSet} takes a snapshot before each attempt at resolution
   * without capture, and reducing the constraints of one such attempt can start inference for a
   * nested expression, which resolves variables of its own and therefore takes a second snapshot of
   * the very same bounds. If the state were stored in the {@link VariableBounds} rather than in the
   * snapshot, the second snapshot would overwrite the first one, and restoring the first one would
   * install the state of the failed attempt that the first snapshot was supposed to undo.
   */
  @Test
  public void restoreOfEarlierSnapshotIsUnaffectedByLaterSave() {
    VariableBounds variableBounds = uninitializedVariableBounds();
    Constraint beforeOuterSnapshot = new DummyConstraint();
    variableBounds.constraints.add(beforeOuterSnapshot);

    VariableBounds.Snapshot outerSnapshot = variableBounds.save();

    Constraint beforeInnerSnapshot = new DummyConstraint();
    variableBounds.constraints.add(beforeInnerSnapshot);
    variableBounds.setHasThrowsBound(true);

    VariableBounds.Snapshot innerSnapshot = variableBounds.save();

    variableBounds.constraints.add(new DummyConstraint());

    // Restoring the inner snapshot undoes only what happened after it was taken.
    variableBounds.restore(innerSnapshot);
    Assert.assertTrue(variableBounds.hasThrowsBound());
    Assert.assertSame(beforeOuterSnapshot, variableBounds.constraints.pop());
    Assert.assertSame(beforeInnerSnapshot, variableBounds.constraints.pop());
    Assert.assertTrue(variableBounds.constraints.isEmpty());

    // Restoring the outer snapshot undoes everything that happened after it was taken, including
    // the state that was current when the inner snapshot was taken.
    variableBounds.restore(outerSnapshot);
    Assert.assertFalse(variableBounds.hasThrowsBound());
    Assert.assertSame(beforeOuterSnapshot, variableBounds.constraints.pop());
    Assert.assertTrue(variableBounds.constraints.isEmpty());
  }

  /**
   * Returns bounds for a variable, where neither the variable nor the context has been initialized.
   * Creating a real variable and a real context requires a running compilation, which is far more
   * than this test needs: {@link VariableBounds}'s constructor only stores the two references.
   *
   * @return bounds for an uninitialized variable
   */
  private static VariableBounds uninitializedVariableBounds() {
    return new VariableBounds(
        uninitialized(Variable.class), uninitialized(Java8InferenceContext.class));
  }
}
