package daikon.diff;

import java.util.logging.Logger;
import java.util.logging.Level;
import daikon.inv.Invariant;
import java.io.*;


/**
 * Prints the differing invariant pairs.
 **/
public class PrintDifferingInvariantsVisitor extends PrintAllVisitor {

  public static final Logger debug = Logger.getLogger ("daikon.diff.DetailedStatisticsVisitor");

  private boolean printUninteresting;

  public PrintDifferingInvariantsVisitor(PrintStream ps, boolean verbose,
                                         boolean printEmptyPpts,
                                         boolean printUninteresting) {
    super(ps, verbose, printEmptyPpts);
    this.printUninteresting = printUninteresting;
  }

  public void visit(InvNode node) {
    Invariant inv1 = node.getInv1();
    Invariant inv2 = node.getInv2();
    if (shouldPrint(inv1, inv2)) {
      super.visit(node);
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
      return printUninteresting;
    }

    int rel = DetailedStatisticsVisitor.determineRelationship(inv1, inv2);
    if (rel == DetailedStatisticsVisitor.REL_SAME_JUST1_JUST2 ||
        rel == DetailedStatisticsVisitor.REL_SAME_UNJUST1_UNJUST2 ||
        rel == DetailedStatisticsVisitor.REL_DIFF_UNJUST1_UNJUST2 ||
        rel == DetailedStatisticsVisitor.REL_MISS_UNJUST1 ||
        rel == DetailedStatisticsVisitor.REL_MISS_UNJUST2) {
      if (debug.isLoggable(Level.FINE)) {
        debug.fine (" Returning false");
      }

      return false;
    }

    if ((inv1 == null || !inv1.isWorthPrinting()) &&
        (inv2 == null || !inv2.isWorthPrinting())) {
      if (debug.isLoggable(Level.FINE)) {
        debug.fine (" Returning false");
      }
      return false;
    }


      if (debug.isLoggable(Level.FINE)) {
        debug.fine (" Returning true");
      }

    return true;
  }
}
