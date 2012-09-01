package daikon.inv;

import daikon.*;

// An interface satisfied by IntComparison, SeqComparison,
// StringComparison, etc.  (Maybe that's the whole list.)
public interface Comparison {
  /**
   * If the invariant is a equality invariant, then its confidence.
   * Otherwise, Invariant.CONFIDENCE_NEVER.
   **/
  public double eq_confidence();

  public VarInfo var1();
  public VarInfo var2();
}
