package daikon.diff;

import java.util.*;
import daikon.inv.*;


/**
 * Comparator for sorting invariants.  If an invariant is an
 * implication, its consequent is used instead of the whole invariant.
 * If the consequents of two invariants are equal, the predicates are
 * compared.  The predicates and consequents themselves are compared
 * using the Comparator c passed to the constructor.  Some examples:
 *
 * this.compare(A->B, A->C) == c.compare(B, C)
 * this.compare(B, A->C) == c.compare(B, C)
 * this.compare(B, C) == c.compare(B, C)
 * this.compare(A->C, B->C) == c.compare(A, B)
 **/
public class ConsequentSortComparator implements Comparator<Invariant> {

  private Comparator<Invariant> c;

  public ConsequentSortComparator(Comparator<Invariant> c) {
    this.c = c;
  }

  public int compare(Invariant inv1, Invariant inv2) {
    Implication imp1 = null;
    Implication imp2 = null;;
    if (inv1 instanceof Implication) {
      imp1 = (Implication) inv1;
      inv1 = imp1.consequent();
    }
    if (inv2 instanceof Implication) {
      imp2 = (Implication) inv2;
      inv2 = imp2.consequent();
    }

    int result = c.compare(inv1, inv2);

    if (result == 0 && imp1 != null && imp2 != null) {
      return c.compare(imp1.predicate(), imp2.predicate());
    } else {
      return result;
    }
  }

}
