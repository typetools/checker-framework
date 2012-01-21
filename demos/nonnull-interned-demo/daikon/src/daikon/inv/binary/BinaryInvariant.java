package daikon.inv.binary;

import daikon.*;
import daikon.inv.*;
import daikon.inv.InvariantStatus;

import java.lang.reflect.*;
import java.util.*;

/**
 * Provides a class that defines the functions that must exist
 * for each two variable invariant.
 **/
public abstract class BinaryInvariant extends Invariant {

  protected BinaryInvariant (PptSlice ppt) {
    super(ppt);
  }

  public abstract InvariantStatus check(Object val1, Object val2,
                                        int mod_index, int count);

  public abstract InvariantStatus add(Object val1, Object val2, int mod_index,
                                      int count);

  /**
   * Applies the variables in the correct order.  If the second variable
   * is an array and the first variable is not, the order of the values
   * is reversed (so that the array is always the first argument).
   */
  public InvariantStatus add_unordered (Object val1, Object val2, int mod_index,
                                        int count) {

    VarInfo v1 = ppt.var_infos[0];
    VarInfo v2 = ppt.var_infos[1];

    if (v2.rep_type.isArray() && !v1.rep_type.isArray())
      return (add (val2, val1, mod_index, count));
    else
      return (add (val1, val2, mod_index, count));

  }

  /**
   * Checks the specified values in the correct order.  If the second value
   * is an array and the first value is not, the order of the values
   * is reversed (so that the array is always the first argument).
   *
   * The values are checked rather than the variables because this is
   * sometimes called on prototype invariants.
   */
  public InvariantStatus check_unordered (Object val1, Object val2,
                                          int mod_index, int count) {


    if (((val2 instanceof long[]) || (val2 instanceof double[])
         || (val2 instanceof String[]))
        && !((val1 instanceof long[]) || (val1 instanceof String[])
              || (val1 instanceof double[])))
      return (check (val2, val1, mod_index, count));
    else
      return (check (val1, val2, mod_index, count));

  }


  /**
   * Returns true if the binary function is symmetric (x,y ==> y,x).
   * Subclasses that are symmetric should override.
   */
  public boolean is_symmetric() {
    return (false);
  }

  /**
   * Returns the swap setting for invariants that support a swap boolean
   * to handle different permutations.  This version should never
   * be called
   */
  public boolean get_swap() {
    throw new Error ("swap called in BinaryInvariant");
  }

  /**
   * Searches for the specified binary invariant (by class) in the
   * specified slice.  Returns null if the invariant is not found
   */
  protected Invariant find (Class<? extends Invariant> cls, VarInfo v1, VarInfo v2) {

    // find the slice containing v1 and v2
    boolean fswap = false;
    PptSlice ppt = null;
    if (v1.varinfo_index > v2.varinfo_index) {
      fswap = true;
      ppt = this.ppt.parent.findSlice (v2, v1);
    } else
      ppt = this.ppt.parent.findSlice (v1, v2);
    if (ppt == null)
      return null;

    // The following is complicated because we are inconsistent in
    // how we handle permutations in binary invariants.  Some
    // invariants (notably the comparison invariants <=, >=, >, etc)
    // use only one permutation, but have two different invariants (eg,
    // < and >) to account for both orders.  Other invariants (notably
    // most of those in Numeric.java.jpp) keep a swap boolean that indicates
    // the order of their arguments.  Still others (such as == and
    // BitwiseComplement) are symmetric and need only track one invariant
    // for each argument pair.
    //
    // The classes with multiple invariants, must provide a static
    // method named swap_class that provides the converse invariant.
    // Symmetric invariants return true from is_symmetric().  Others
    // must support the get_swap() method that returns the current
    // swap setting.

    // If the specified invariant has a different class when swapped
    // find that class.
    boolean swap_class = true;
    try {
      Method swap_method = cls.getMethod ("swap_class", (Class[])null);
      if (fswap)
        cls = (Class<? extends Invariant>) swap_method.invoke (null, (Object[])null); // unchecked cast
    } catch (Exception e) {
      swap_class = false;
    }

    // Loop through each invariant, looking for the matching class
    for (Invariant inv : ppt.invs) {
      BinaryInvariant bi = (BinaryInvariant) inv;
      if (bi.getClass() == cls) {
        if (bi.is_symmetric() || swap_class)
          return (bi);
        else if (bi.get_swap() == fswap)
          return (bi);
      }
    }

    return (null);
  }


}
