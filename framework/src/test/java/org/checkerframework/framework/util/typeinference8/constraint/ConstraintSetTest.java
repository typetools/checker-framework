package org.checkerframework.framework.util.typeinference8.constraint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.BugInCF;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests {@link ConstraintSet}: that {@link ConstraintSet#TRUE} and {@link
 * ConstraintSet#TRUE_ANNO_FAIL} cannot be modified, that the ordering and deduplication of
 * constraints is as documented, and that adding a constraint takes constant time.
 *
 * <p>{@code TRUE} and {@code TRUE_ANNO_FAIL} are static singletons that are shared by every
 * inference problem, so modifying either of them would corrupt every subsequent inference.
 */
public class ConstraintSetTest {

  /** Creates a new ConstraintSetTest. */
  public ConstraintSetTest() {}

  /** A constraint that is only ever used as an argument to a modification method. */
  private static class DummyConstraint implements Constraint {

    /** Creates a new DummyConstraint. */
    DummyConstraint() {}

    @Override
    public Kind getKind() {
      return Kind.SUBTYPE;
    }

    @Override
    public ReductionResult reduce(Java8InferenceContext context) {
      throw new AssertionError("DummyConstraint.reduce() should never be called.");
    }
  }

  /**
   * A constraint whose {@code equals} and {@code hashCode} are determined by an integer, and which
   * counts the calls to its {@code equals}.
   */
  private static class NumberedConstraint implements Constraint {

    /** The number of calls to {@link #equals} on any {@code NumberedConstraint}. */
    static int equalsCalls = 0;

    /**
     * Two {@code NumberedConstraint}s are equal if their {@code id}s are equal. It is not final, so
     * that a test can change a constraint's equality and hash code while the constraint is in a
     * constraint set, the way {@code TypeConstraint.applyInstantiations} does.
     */
    int id;

    /**
     * Creates a new NumberedConstraint.
     *
     * @param id determines equality
     */
    NumberedConstraint(int id) {
      this.id = id;
    }

    @Override
    public Kind getKind() {
      return Kind.SUBTYPE;
    }

    @Override
    public ReductionResult reduce(Java8InferenceContext context) {
      throw new AssertionError("NumberedConstraint.reduce() should never be called.");
    }

    @Override
    public boolean equals(Object other) {
      equalsCalls++;
      return other instanceof NumberedConstraint && ((NumberedConstraint) other).id == id;
    }

    @Override
    public int hashCode() {
      return id;
    }

    @Override
    public String toString() {
      return "NumberedConstraint(" + id + ")";
    }
  }

  /**
   * Removes and returns every constraint in {@code constraintSet}, in the order that {@link
   * ConstraintSet#pop} returns them.
   *
   * @param constraintSet a constraint set; it is empty when this method returns
   * @return the constraints of {@code constraintSet}, in pop order
   */
  private static List<Constraint> drain(ConstraintSet constraintSet) {
    List<Constraint> result = new ArrayList<>();
    while (!constraintSet.isEmpty()) {
      result.add(constraintSet.pop());
    }
    return result;
  }

  /**
   * If {@code modification} does not throw {@link BugInCF}, adds {@code description} to {@code
   * failures}.
   *
   * @param failures the descriptions of the modification methods that did not throw {@link BugInCF}
   * @param description a description of {@code modification}
   * @param modification an action that attempts to modify a constraint set
   */
  private static void checkThrowsBugInCF(
      List<String> failures, String description, Runnable modification) {
    try {
      modification.run();
    } catch (BugInCF e) {
      return;
    }
    failures.add(description);
  }

  /**
   * Asserts that every modification method of {@code constraintSet} throws {@link BugInCF}, and
   * that {@code constraintSet} is still empty afterward.
   *
   * @param name the name of {@code constraintSet}, for failure messages
   * @param constraintSet the constraint set that must not be modifiable
   */
  private static void assertUnmodifiable(String name, ConstraintSet constraintSet) {
    ConstraintSet other = new ConstraintSet(new DummyConstraint());
    List<String> failures = new ArrayList<>();

    checkThrowsBugInCF(failures, name + ".add()", () -> constraintSet.add(new DummyConstraint()));
    checkThrowsBugInCF(
        failures, name + ".addAll(ConstraintSet)", () -> constraintSet.addAll(other));
    checkThrowsBugInCF(
        failures,
        name + ".addAll(Collection)",
        () -> constraintSet.addAll(Collections.singletonList(new DummyConstraint())));
    checkThrowsBugInCF(failures, name + ".pop()", () -> constraintSet.pop());
    checkThrowsBugInCF(failures, name + ".push()", () -> constraintSet.push(new DummyConstraint()));
    checkThrowsBugInCF(failures, name + ".pushAll()", () -> constraintSet.pushAll(other));
    checkThrowsBugInCF(failures, name + ".remove()", () -> constraintSet.remove(other));
    checkThrowsBugInCF(
        failures, name + ".applyInstantiations()", () -> constraintSet.applyInstantiations());

    Assert.assertEquals(
        "these methods did not throw BugInCF: " + failures, Collections.emptyList(), failures);
    Assert.assertTrue(name + " is no longer empty", constraintSet.isEmpty());
    Assert.assertEquals(name, constraintSet.toString());
  }

  /** Tests that {@link ConstraintSet#TRUE} cannot be modified. */
  @Test
  public void trueIsUnmodifiable() {
    assertUnmodifiable("TRUE", ConstraintSet.TRUE);
  }

  /** Tests that {@link ConstraintSet#TRUE_ANNO_FAIL} cannot be modified. */
  @Test
  public void trueAnnoFailIsUnmodifiable() {
    assertUnmodifiable("TRUE_ANNO_FAIL", ConstraintSet.TRUE_ANNO_FAIL);
  }

  /**
   * Adding a constraint takes constant time, so building a constraint set takes time linear in its
   * size. An implementation that searches the already-added constraints for an equal one takes
   * quadratic time; because {@code Constraint.equals} walks the structure of the constrained types,
   * that search is expensive as well as quadratic.
   */
  @Test
  public void addingAConstraintTakesConstantTime() {
    int size = 500;
    NumberedConstraint.equalsCalls = 0;
    ConstraintSet constraintSet = new ConstraintSet();
    for (int i = 0; i < size; i++) {
      constraintSet.add(new NumberedConstraint(i));
    }
    // A linear-time implementation makes no `equals` call at all, because every constraint has a
    // distinct hash code.  A quadratic implementation makes size*(size-1)/2 of them.  The bound is
    // `size` rather than 0 to permit an implementation that resolves hash collisions with `equals`.
    Assert.assertTrue(
        "adding "
            + size
            + " constraints made "
            + NumberedConstraint.equalsCalls
            + " calls to Constraint.equals",
        NumberedConstraint.equalsCalls <= size);
  }

  /** Tests that {@code add} appends and that {@code pop} removes the constraint added first. */
  @Test
  public void addAppendsAndPopRemovesTheOldest() {
    Constraint c0 = new NumberedConstraint(0);
    Constraint c1 = new NumberedConstraint(1);
    Constraint c2 = new NumberedConstraint(2);
    ConstraintSet constraintSet = new ConstraintSet(c0, c1);
    constraintSet.add(c2);
    Assert.assertEquals(Arrays.asList(c0, c1, c2), drain(constraintSet));
  }

  /** Tests that {@code push} adds to the beginning of a constraint set. */
  @Test
  public void pushPrepends() {
    Constraint c0 = new NumberedConstraint(0);
    Constraint c1 = new NumberedConstraint(1);
    Constraint c2 = new NumberedConstraint(2);
    ConstraintSet constraintSet = new ConstraintSet(c0, c1);
    constraintSet.push(c2);
    Assert.assertEquals(Arrays.asList(c2, c0, c1), drain(constraintSet));
  }

  /** Tests that {@code pushAll} prepends while maintaining the order of the pushed constraints. */
  @Test
  public void pushAllPrependsInOrder() {
    Constraint c0 = new NumberedConstraint(0);
    Constraint c1 = new NumberedConstraint(1);
    Constraint c2 = new NumberedConstraint(2);
    ConstraintSet constraintSet = new ConstraintSet(c0);
    constraintSet.pushAll(new ConstraintSet(c1, c2));
    Assert.assertEquals(Arrays.asList(c1, c2, c0), drain(constraintSet));
  }

  /** Tests that a constraint set does not contain two constraints that are equal. */
  @Test
  public void equalConstraintsAreNotAddedTwice() {
    ConstraintSet constraintSet = new ConstraintSet(new NumberedConstraint(0));
    constraintSet.add(new NumberedConstraint(0));
    constraintSet.push(new NumberedConstraint(0));
    constraintSet.addAll(new ConstraintSet(new NumberedConstraint(0)));
    constraintSet.addAll(Collections.singletonList(new NumberedConstraint(0)));
    constraintSet.pushAll(new ConstraintSet(new NumberedConstraint(0)));
    Assert.assertEquals(1, drain(constraintSet).size());
  }

  /**
   * Tests that a popped constraint can be added again. This fails if {@code pop} removes a
   * constraint from the constraint set but not from whatever data structure answers "is a
   * constraint equal to this one already in this set?".
   */
  @Test
  public void aPoppedConstraintCanBeAddedAgain() {
    Constraint c0 = new NumberedConstraint(0);
    ConstraintSet constraintSet = new ConstraintSet(c0);
    Assert.assertEquals(c0, constraintSet.pop());
    constraintSet.add(new NumberedConstraint(0));
    Assert.assertEquals(Collections.singletonList(c0), drain(constraintSet));
  }

  /**
   * Tests that a removed constraint can be added again. This fails if {@code remove} removes a
   * constraint from the constraint set but not from whatever data structure answers "is a
   * constraint equal to this one already in this set?".
   */
  @Test
  public void aRemovedConstraintCanBeAddedAgain() {
    Constraint c0 = new NumberedConstraint(0);
    Constraint c1 = new NumberedConstraint(1);
    ConstraintSet constraintSet = new ConstraintSet(c0, c1);
    constraintSet.remove(new ConstraintSet(new NumberedConstraint(0)));
    constraintSet.add(new NumberedConstraint(0));
    Assert.assertEquals(Arrays.asList(c1, c0), drain(constraintSet));
  }

  /** Tests that {@code remove} of a constraint set removes all of its constraints. */
  @Test
  public void removeOfThisEmptiesTheConstraintSet() {
    ConstraintSet constraintSet = new ConstraintSet(new NumberedConstraint(0));
    constraintSet.remove(constraintSet);
    Assert.assertTrue(constraintSet.isEmpty());
    constraintSet.add(new NumberedConstraint(0));
    Assert.assertEquals(1, drain(constraintSet).size());
  }

  /**
   * Applying instantiations changes the equality and hash code of a constraint, so a constraint set
   * must not depend on a constraint's hash code from before {@code applyInstantiations} was called.
   */
  @Test
  public void applyInstantiationsPreservesMembership() {
    NumberedConstraint c0 = new NumberedConstraint(0);
    ConstraintSet constraintSet = new ConstraintSet(c0);
    // Change the hash code of a constraint that is in the set, as applying an instantiation to a
    // TypeConstraint does.  The constraint is mutated directly, rather than by
    // constraintSet.applyInstantiations(), because NumberedConstraint is not a TypeConstraint.
    c0.id = 1;
    constraintSet.applyInstantiations();
    constraintSet.add(new NumberedConstraint(1));
    Assert.assertEquals(Collections.singletonList(c0), drain(constraintSet));
  }

  /**
   * Applying instantiations can make two constraints in a set equal to one another. Only one of
   * them is retained, because a constraint set does not contain two constraints that are equal.
   */
  @Test
  public void applyInstantiationsDiscardsDuplicates() {
    NumberedConstraint c0 = new NumberedConstraint(0);
    NumberedConstraint c1 = new NumberedConstraint(1);
    NumberedConstraint c2 = new NumberedConstraint(2);
    ConstraintSet constraintSet = new ConstraintSet(c0, c1, c2);
    // Make two constraints that are in the set equal, as applying an instantiation to a
    // TypeConstraint can do.  The constraints are mutated directly, rather than by
    // constraintSet.applyInstantiations(), because NumberedConstraint is not a TypeConstraint.
    c1.id = 0;
    constraintSet.applyInstantiations();
    // The constraint that was added first is the one that is retained.
    List<Constraint> constraints = drain(constraintSet);
    Assert.assertEquals(2, constraints.size());
    Assert.assertSame(c0, constraints.get(0));
    Assert.assertSame(c2, constraints.get(1));
  }

  /**
   * Removing constraints takes time linear in the sizes of the two constraint sets. An
   * implementation that searches this set for each constraint that is being removed takes quadratic
   * time; because {@code Constraint.equals} walks the structure of the constrained types, that
   * search is expensive as well as quadratic.
   */
  @Test
  public void removingConstraintsTakesLinearTime() {
    int size = 500;
    ConstraintSet constraintSet = new ConstraintSet();
    ConstraintSet toRemove = new ConstraintSet();
    for (int i = 0; i < size; i++) {
      constraintSet.add(new NumberedConstraint(i));
      if (i % 2 == 0) {
        toRemove.add(new NumberedConstraint(i));
      }
    }
    NumberedConstraint.equalsCalls = 0;
    constraintSet.remove(toRemove);
    int equalsCalls = NumberedConstraint.equalsCalls;
    Assert.assertEquals(size / 2, drain(constraintSet).size());
    // The current implementation makes two `equals` calls per removed constraint, or `size` calls
    // in all.  A quadratic implementation makes about size*size/4 of them.
    Assert.assertTrue(
        "removing "
            + (size / 2)
            + " of "
            + size
            + " constraints made "
            + equalsCalls
            + " calls to Constraint.equals",
        equalsCalls <= 2 * size);
  }
}
