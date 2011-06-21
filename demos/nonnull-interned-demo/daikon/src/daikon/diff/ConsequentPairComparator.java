package daikon.diff;

import java.util.*;
import daikon.inv.*;

/**
 * Comparator for pairing invariants.  In an invariant in set2 is an
 * implication, its consequent is used instead of the whole invariant.
 * In set1, the whole invariant is always used.  Some examples:
 *
 * this.compare(A, B->A) == c.compare(A, A)
 * this.compare(C, D) == c.compare(C, D)
 **/
public class ConsequentPairComparator implements Comparator<Invariant> {

  private Comparator<Invariant> c;

  public ConsequentPairComparator(Comparator<Invariant> c) {
    this.c = c;
  }

  public int compare(Invariant inv1, Invariant inv2) {
    if (inv2 instanceof Implication) {
      Implication imp2 = (Implication) inv2;
      inv2 = imp2.consequent();
    }

    return c.compare(inv1, inv2);
  }

}
