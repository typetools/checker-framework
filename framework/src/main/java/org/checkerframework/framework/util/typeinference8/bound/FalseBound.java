package org.checkerframework.framework.util.typeinference8.bound;

import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;

public class FalseBound implements ReductionResult {

  public FalseBound() {}

  @Override
  public String toString() {
    return "FalseBound";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o != null && getClass() == o.getClass();
  }

  @Override
  public int hashCode() {
    return 0;
  }
}
