package daikon.inv.unary;

import daikon.inv.*;
import daikon.PptSlice;
import utilMDE.Assert;
import daikon.inv.InvariantStatus;

/**
 * Exists simply to provide the do-nothing resusurrect_done method and
 * abstract add method.
 **/
public abstract class UnaryInvariant
  extends Invariant
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  /** Pass-through. */
  protected UnaryInvariant(PptSlice ppt) {
    super(ppt);
  }

  /** @return this */
  protected Invariant resurrect_done(int[] permutation) {
    Assert.assertTrue(permutation.length == 1);
    Assert.assertTrue(permutation[0] == 0);
    return this;
  }

  public abstract InvariantStatus add(Object val, int mod_index, int count);

  public abstract InvariantStatus check(Object val1, int mod_index, int count);

}
