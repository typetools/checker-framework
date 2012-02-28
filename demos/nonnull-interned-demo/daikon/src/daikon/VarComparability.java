package daikon;

import utilMDE.*;

import java.util.logging.Logger;

// Internally, we use the names "array[]", "array[]-element", and
// "array[]-indexn".  These may be different depending on the programming
// language; for instance, C uses "*array" in place of "array[]-element".


/**
 * Represents the comparability of variables, including methods to
 * determine if two VarComparabilities are comparable.
 * VarComparability types have two formats: implicit and none.<p>
 *
 * A VarComparabilityImplicit is an arbitrary string, and comparisons
 * succeed exactly if the two VarComparabilitys are identical.<p>
 *
 * VarComparabilityNone means no comparability information was provided.<p>
 **/
public abstract class VarComparability {

  /** Debug tracer. **/
  public static final Logger debug =
    Logger.getLogger("daikon.VarComparability");


  public static final int NONE = 0;
  public static final int IMPLICIT = 1;

  /**
   * Create a VarComparability representing the given arguments with
   * respect to a variable.
   * @param format the type of comparability, either NONE or IMPLICIT
   * @param rep a regular expression indicating
   * how to match.  The form is "(a)[b][c]..." where each variable is
   * string (or number) that is a UID for a basic type.  a is the type
   * of the element, b is the type of the first index, c the type of
   * the second, etc.  Index variables only apply if this is an array.
   * @param vartype the declared type of the variable
   **/
  public static VarComparability parse(int format, String rep, ProglangType vartype) {
    if (format == NONE) {
      return VarComparabilityNone.parse(rep, vartype);
    } else if (format == IMPLICIT) {
      return VarComparabilityImplicit.parse(rep, vartype);
    } else {
      throw new IllegalArgumentException("bad format argument " + format
                      + " should have been in {0, 1, 2}");
    }
  }

  /**
   * Create a VarComparability based on comparabilities of indices.
   * @return a new comparability that is an array with the same dimensionality
   * and indices as given, but with a different element type.
   *
   * @param elemTypeName the new type of the elements of return value.
   * @param old the varcomparability that this is derived from; has
   * the same indices as this.
   **/
  public static VarComparability makeComparabilitySameIndices (String elemTypeName,
                                                               VarComparability old) {
    if (old instanceof VarComparabilityNone) {
      return VarComparabilityNone.it;
    } else {
      throw new Error ("makeComparabilitySameIndices not implemented for implicit comparables");
    }
  }

  public static VarComparability makeAlias(VarInfo vi) {
    return vi.comparability.makeAlias();
  }
  public abstract VarComparability makeAlias();

  public abstract VarComparability elementType();
  public abstract VarComparability indexType(int dim);

  /** Return the comparability for the length of this string**/
  public abstract VarComparability string_length_type();

  /**
   * Returns true if this is comparable to everything else.
   */
  public abstract boolean alwaysComparable();

  /** Returns whether two variables are comparable. **/
  public static boolean comparable(VarInfo v1, VarInfo v2) {
    return comparable(v1.comparability, v2.comparability);
  }

  /** Returns whether two comparabilities are comparable. **/
  public static boolean comparable (VarComparability type1,
                                    VarComparability type2) {

    if (type1 != null && type2 != null && type1.getClass() != type2.getClass())
      throw new Error(String.format ("Trying to compare VarComparabilities " +
                      "of different types: %s (%s) and %s (%s)", type1,
                      type1.getClass(), type2, type2.getClass()));

    if (type1 instanceof VarComparabilityNone || type1 == null || type2 == null) {
      return VarComparabilityNone.comparable ((VarComparabilityNone)type1,
                                              (VarComparabilityNone)type2);
    } else if (type1 instanceof VarComparabilityImplicit) {
        return VarComparabilityImplicit.comparable
          ((VarComparabilityImplicit)type1,
           (VarComparabilityImplicit)type2);
    } else {
      throw new Error("Unrecognized subtype of VarComparability: " + type1);
    }
  }

  /**
   * In general, if two items are comparable, they can be placed in the
   * same equality set.  This is not always true for some comparabilities
   * (because they are not always transitive).  They can override this
   * method to provide the correct results
   */
  public boolean equality_set_ok (VarComparability other) {
    return comparable (this, other);
  }

}
