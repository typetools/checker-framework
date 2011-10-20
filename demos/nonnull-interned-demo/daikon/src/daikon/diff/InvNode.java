package daikon.diff;

import daikon.inv.Invariant;
import utilMDE.*;

/**
 * Contains a pair of Invariants.  Resides in the third level of the tree.
 * Has no children.
 **/
public class InvNode extends Node<Invariant,NoType> {

  /** Either inv1 or inv2 may be null, but not both. **/
  public InvNode(Invariant inv1, Invariant inv2) {
    super(new Pair<Invariant,Invariant>(inv1, inv2));
    Assert.assertTrue(!(inv1 == null && inv2 == null),
                  "Both invariants may not be null");
  }

  public Invariant getInv1() {
    return getUserLeft();
  }

  public Invariant getInv2() {
    return getUserRight();
  }

  public void accept(Visitor v) {
    v.visit(this);
  }

}
