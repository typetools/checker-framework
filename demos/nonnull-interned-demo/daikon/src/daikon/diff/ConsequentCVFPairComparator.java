package daikon.diff;

import java.util.*;
import daikon.inv.*;

/**
 * Comparator for sorting invariants.  Uses the
 * ConsequentPairComparator, initialized with the
 * ClassVarnameFormulaComparator.  See the documentation for those two
 * classes to figure out what this class does.
 **/
public class ConsequentCVFPairComparator implements Comparator<Invariant> {
  private Comparator<Invariant> c = new ConsequentPairComparator
    (new Invariant.ClassVarnameFormulaComparator());

  public int compare(Invariant inv1, Invariant inv2) {
    return c.compare(inv1, inv2);
  }
}
