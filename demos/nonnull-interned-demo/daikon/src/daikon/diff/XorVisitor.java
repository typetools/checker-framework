package daikon.diff;

import daikon.*;
import daikon.inv.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Computes A xor B, where A and B are the two sets of invariants.
 **/
public class XorVisitor extends DepthFirstVisitor {

  private InvMap result = new InvMap();
  private PptTopLevel currentPpt;

  public static final Logger debug = Logger.getLogger ("daikon.diff.XorVisitor");

  /**
   * Every node has at least one non-null ppt.  Add one of the
   * non-null ppt to the result.
   **/
  public void visit(PptNode node) {
    PptTopLevel ppt1 = node.getPpt1();
    PptTopLevel ppt2 = node.getPpt2();
    PptTopLevel pptNonNull = (ppt1 != null ? ppt1 : ppt2);
    result.addPpt(pptNonNull);
    currentPpt = pptNonNull;
    super.visit(node);
  }

  /**
   * If one invariant is null and the other is not, add the non-null
   * invariant to the result set.
   **/
  public void visit(InvNode node) {
    Invariant inv1 = node.getInv1();
    Invariant inv2 = node.getInv2();
    if (debug.isLoggable(Level.FINE)) {
      debug.fine ("visit: "
                    + ((inv1 != null) ? inv1.ppt.parent.name() : "NULL") + " "
                    + ((inv1 != null) ? inv1.repr() : "NULL") + " - "
                    + ((inv2 != null) ? inv2.repr() : "NULL"));
    }
    if (shouldAddInv1(inv1, inv2)) {
      result.add(currentPpt, inv1);
    } else if (shouldAddInv2(inv1, inv2)) {
      result.add(currentPpt, inv2);
    }
  }


  private static boolean shouldAddInv1(Invariant inv1, Invariant inv2) {
    return ((inv1 != null) && (inv2 == null));
  }

  private static boolean shouldAddInv2(Invariant inv1, Invariant inv2) {
    return ((inv2 != null) && (inv1 == null));
  }


  /** Returns the InvMap generated as a result of the traversal. **/
  public InvMap getResult() {
    return result;
  }

}
