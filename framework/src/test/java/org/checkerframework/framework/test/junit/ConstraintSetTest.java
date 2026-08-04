package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.types.Dependencies;
import org.checkerframework.javacutil.BugInCF;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link ConstraintSet}. */
public class ConstraintSetTest {

  @Test
  public void getClosedSubsetOfEmptySet() {
    Assert.assertThrows(
        BugInCF.class,
        () -> ConstraintSet.getClosedSubset(new ConstraintSet(), new Dependencies()));
  }
}
