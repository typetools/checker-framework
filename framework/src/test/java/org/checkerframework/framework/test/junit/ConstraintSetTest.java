package org.checkerframework.framework.test.junit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;
import org.checkerframework.framework.util.typeinference8.types.Dependencies;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.BugInCF;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that {@link ConstraintSet} maintains the invariant documented on its list of constraints:
 * "It does not contain constraints that are equal."
 */
public class ConstraintSetTest {

  /**
   * A constraint that is equal to another {@code NamedConstraint} with the same name. This test
   * only exercises {@code ConstraintSet}'s bookkeeping, so this constraint is never reduced.
   */
  private static class NamedConstraint implements Constraint {

    /** The name of this constraint; two constraints are equal if their names are equal. */
    private final String name;

    /**
     * Creates a constraint with the given name.
     *
     * @param name the name of this constraint
     */
    NamedConstraint(String name) {
      this.name = name;
    }

    @Override
    public Kind getKind() {
      return Kind.SUBTYPE;
    }

    @Override
    public NamedConstraint copy() {
      // A NamedConstraint is immutable, so it is its own copy.
      return this;
    }

    @Override
    public ReductionResult reduce(Java8InferenceContext context) {
      throw new UnsupportedOperationException("ConstraintSetTest never reduces a constraint.");
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return other instanceof NamedConstraint && ((NamedConstraint) other).name.equals(name);
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      return name;
    }
  }

  /**
   * Removes and returns all the constraints in {@code set}, in order. This is the only way to
   * observe the contents of a {@code ConstraintSet}, which exposes no accessor.
   *
   * @param set a constraint set; it is emptied by this method
   * @return the constraints that were in {@code set}, in order
   */
  private static List<Constraint> drain(ConstraintSet set) {
    List<Constraint> result = new ArrayList<>();
    while (!set.isEmpty()) {
      result.add(set.pop());
    }
    return result;
  }

  /**
   * {@code addAll(Collection)} must not add a constraint that is equal to one that the collection
   * contains earlier.
   */
  @Test
  public void addAllCollectionSkipsDuplicatesWithinTheCollection() {
    Constraint a = new NamedConstraint("a");
    Constraint aAgain = new NamedConstraint("a");
    Constraint b = new NamedConstraint("b");
    Collection<Constraint> constraints = Arrays.asList(a, b, aAgain);

    ConstraintSet set = new ConstraintSet();
    set.addAll(constraints);

    Assert.assertEquals(Arrays.asList(a, b), drain(set));
  }

  /**
   * {@code addAll(Collection)} must not add a constraint that is equal to one already in the
   * constraint set.
   */
  @Test
  public void addAllCollectionSkipsDuplicatesOfExistingConstraints() {
    Constraint a = new NamedConstraint("a");
    Constraint b = new NamedConstraint("b");

    ConstraintSet set = new ConstraintSet();
    set.add(a);
    set.add(b);
    set.addAll(Arrays.asList(new NamedConstraint("b"), new NamedConstraint("a")));

    Assert.assertEquals(Arrays.asList(a, b), drain(set));
  }

  /**
   * {@code addAll(Collection)} must add every constraint that is not a duplicate, in order, after
   * the constraints already in the set.
   */
  @Test
  public void addAllCollectionAddsNonDuplicatesInOrder() {
    Constraint a = new NamedConstraint("a");
    Constraint b = new NamedConstraint("b");
    Constraint c = new NamedConstraint("c");

    ConstraintSet set = new ConstraintSet();
    set.add(a);
    set.addAll(Arrays.asList(b, new NamedConstraint("a"), c));

    Assert.assertEquals(Arrays.asList(a, b, c), drain(set));
  }

  /**
   * {@code addAll(Collection)} must behave like {@code addAll(ConstraintSet)}, which already routes
   * through {@code add}.
   */
  @Test
  public void addAllCollectionAgreesWithAddAllConstraintSet() {
    List<Constraint> constraints =
        Arrays.asList(
            new NamedConstraint("a"),
            new NamedConstraint("b"),
            new NamedConstraint("a"),
            new NamedConstraint("c"),
            new NamedConstraint("b"));

    ConstraintSet viaCollection = new ConstraintSet();
    viaCollection.addAll(constraints);

    ConstraintSet source = new ConstraintSet();
    constraints.forEach(source::add);
    ConstraintSet viaConstraintSet = new ConstraintSet();
    viaConstraintSet.addAll(source);

    Assert.assertEquals(drain(viaConstraintSet), drain(viaCollection));
  }

  /**
   * {@code clear()} must remove every constraint, and must leave a set that behaves like a newly
   * created one.
   */
  @Test
  public void clearRemovesEveryConstraint() {
    ConstraintSet set = new ConstraintSet();
    set.addAll(Arrays.asList(new NamedConstraint("a"), new NamedConstraint("b")));

    set.clear();

    Assert.assertTrue(set.isEmpty());

    // A constraint that the set contained before it was cleared is no longer a duplicate.
    Constraint a = new NamedConstraint("a");
    set.add(a);
    Assert.assertEquals(Arrays.asList(a), drain(set));
  }

  @Test
  public void getClosedSubsetOfEmptySet() {
    Assert.assertThrows(
        BugInCF.class,
        () -> ConstraintSet.getClosedSubset(new ConstraintSet(), new Dependencies()));
  }
}
