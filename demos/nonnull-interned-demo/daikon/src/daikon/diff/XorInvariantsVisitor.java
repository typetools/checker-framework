package daikon.diff;

import daikon.inv.Invariant;
import java.io.*;
import daikon.*;

/** <B>XorInvariantsVisitor</B> is a visitor that performs a
 * standard Diff on two PptMaps, that is, finds the set of
 * Invariants in the XOR set of two PptMaps.  However, while
 * those XOR Invariants were the end product of standard diff,
 * this visitor is useful when the XOR set is a means to an
 * end, since you get back a data structure containing the
 * XOR set. <P> Currently, this visitor actually modifies
 * the first of the two PptMaps.  This might be an undesirable
 * design call, but creating a PptMap from scratch is difficult
 * given the constraining creational pattern in place.
 **/
public class XorInvariantsVisitor extends PrintDifferingInvariantsVisitor {

    public XorInvariantsVisitor (PrintStream ps, boolean verbose,
                                 boolean printEmptyPpts,
                                 boolean printUninteresting) {
        super(ps, verbose, printEmptyPpts, printUninteresting);
    }

    public void visit (PptNode node) {
        super.visit(node);

    }

    public void visit (InvNode node) {
	Invariant inv1 = node.getInv1();
	Invariant inv2 = node.getInv2();
	// do nothing if they are unique

        if (shouldPrint (inv1, inv2)) {
            // do nothing, keep both
        }

        else {
            if (inv1 != null) {
                inv1.ppt.removeInvariant(inv1);
            }

            if (inv2 != null) {
                inv2.ppt.removeInvariant(inv2);
            }

        }
     }


  /**
   * Returns true if the pair of invariants should be printed,
   * depending on their type, relationship, and printability.
   **/
  protected boolean shouldPrint(Invariant inv1, Invariant inv2) {
      int type = DetailedStatisticsVisitor.determineType(inv1, inv2);
      if (type == DetailedStatisticsVisitor.TYPE_NULLARY_UNINTERESTING ||
          type == DetailedStatisticsVisitor.TYPE_UNARY_UNINTERESTING) {
          return false;
      }

      int rel = DetailedStatisticsVisitor.determineRelationship(inv1, inv2);
      if (rel == DetailedStatisticsVisitor.REL_SAME_JUST1_JUST2 ||
          rel == DetailedStatisticsVisitor.REL_SAME_UNJUST1_UNJUST2 ||
          rel == DetailedStatisticsVisitor.REL_DIFF_UNJUST1_UNJUST2 ||
          rel == DetailedStatisticsVisitor.REL_MISS_UNJUST1 ||
          rel == DetailedStatisticsVisitor.REL_MISS_UNJUST2) {
          return false;
      }

      if ((inv1 == null || !inv1.isWorthPrinting()) &&
          (inv2 == null || !inv2.isWorthPrinting())) {
          return false;
      }

      return true;
  }
}
