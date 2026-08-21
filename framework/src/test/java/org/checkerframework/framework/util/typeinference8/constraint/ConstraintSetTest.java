package org.checkerframework.framework.util.typeinference8.constraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.BugInCF;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that {@link ConstraintSet#TRUE} and {@link ConstraintSet#TRUE_ANNO_FAIL} cannot be
 * modified. Those two constraint sets are static singletons that are shared by every inference
 * problem, so modifying either of them would corrupt every subsequent inference.
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
    public DummyConstraint copy() {
      // A DummyConstraint is immutable, so it is its own copy.
      return this;
    }

    @Override
    public ReductionResult reduce(Java8InferenceContext context) {
      throw new AssertionError("DummyConstraint.reduce() should never be called.");
    }
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
    checkThrowsBugInCF(failures, name + ".clear()", () -> constraintSet.clear());
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
}
