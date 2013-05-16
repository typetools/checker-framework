package daikon.inv.ternary;

import daikon.PptSlice;
import daikon.inv.Invariant;
import utilMDE.Assert;
import daikon.inv.InvariantStatus;

/**
 * Exists simply to provide a more intelligent resusurrect_done method.
 **/
public abstract class TernaryInvariant
  extends Invariant
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  /** Pass-through. */
  protected TernaryInvariant(PptSlice ppt) {
    super(ppt);
  }

  // Check if swap occurred and call one of the other methods
  protected Invariant resurrect_done(int[] permutation) {
    Assert.assertTrue(permutation.length == 3);
    // Assert.assertTrue(ArraysMDE.fn_is_permutation(permutation));
    throw new Error("to implement");
  }

  public abstract InvariantStatus add(Object val1, Object val2, Object val3, int mod_index, int count);

  public abstract InvariantStatus check(Object val1, Object val2, Object val3, int mod_index, int count);

}
