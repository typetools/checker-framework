package org.checkerframework.framework.util.typeinference.constraint;

import java.util.Set;

/**
 * AFReducer implementations reduce AFConstraints into one or more "simpler" AFConstraints until
 * these constraints are irreducible.
 *
 * @see
 *     org.checkerframework.framework.util.typeinference.constraint.AFConstraint#isIrreducible(java.util.Set)
 *     <p>There is one AFReducer for each type of AFConstraint.
 */
public interface AFReducer {

  /**
   * Determines if the input constraint should be handled by this reducer. If so: Reduces the
   * constraint into one or more new constraints. Any new constraint that can still be reduced is
   * placed in newConstraints. New irreducible constraints are placed in finish. Return true Return
   * false (indicating that some other reducer needs to handle this constraint) If false is
   * returned, the reducer should NOT place any constraints in newConstraints or finished
   *
   * @param constraint the constraint to reduce
   * @param newConstraints the new constraints that may still need to be reduced
   * @return true if the input constraint was handled by this reducer, false otherwise
   */
  public boolean reduce(AFConstraint constraint, Set<AFConstraint> newConstraints);
}
