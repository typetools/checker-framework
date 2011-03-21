package daikon.diff;

import daikon.*;
import daikon.inv.*;

/**
 * Computes A union B, where A and B are the two sets of invariants.
 **/
public class UnionVisitor extends DepthFirstVisitor {

  private InvMap result = new InvMap();
  private PptTopLevel currentPpt;

  public InvMap getResult() {
    return result;
  }

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
   * If only one invariant is non-null, always add it. If two
   * invariants are non-null, add the invariant with the better
   * (higher) confidence.
   **/
  public void visit(InvNode node) {
    Invariant inv1 = node.getInv1();
    Invariant inv2 = node.getInv2();
    if (inv1 == null) {
      result.add(currentPpt, inv2);
    } else if (inv2 == null) {
      result.add(currentPpt, inv1);
    } else {
      if (inv1.getConfidence() >= inv2.getConfidence()) {
        result.add(currentPpt, inv1);
      } else {
        result.add(currentPpt, inv2);
      }
    }
  }

}
