package daikon.derive;

import daikon.*;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Structure that represents a derivation; can generate values and
 * derived variables from base variables.  A Derivation has a set of
 * base VarInfo from which the Derivation is derived.  Use
 * getVarInfo() to get the VarInfo representation of this Derivation.
 * When we want the actual value of this derivation, we pass in a
 * ValueTuple; the Derivation picks out the values of its base
 * variables and finds the value of the derived variable.  Use
 * computeValueandModified() to get value.  Derivations are created by
 * DerivationFactory.
 **/
public abstract class Derivation
  implements Serializable, Cloneable
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // This definition is here so that it will show up in the manual
  // with the other options for controlling derived variables
  /**
   * Boolean.  If true, Daikon will not create any derived variables.
   * Derived variables, which are combinations of variables that appeared in
   * the program, like <code>array[index]</code> if <code>array</code> and
   * <code>index</code> appeared, can
   * increase the number of properties Daikon finds, especially over
   * sequences. However, derived variables increase Daikon's time and
   * memory usage, sometimes dramatically. If false, individual kinds of
   * derived variables can be enabled or disabled individually using
   * configuration options under <samp>daikon.derive</samp>.
   **/
  public static boolean dkconfig_disable_derived_variables = false;

  /**
   * Debug tracer.
   **/
  public static final Logger debug = Logger.getLogger("daikon.derive.Derivation");

  // This is static, so we can't mention it here.
  // It's in DerivationFactory, though. // really?
  // public boolean applicable();


  // This is essentially a clone() method that also switches the variables.
  public abstract Derivation switchVars(VarInfo[] old_vars, VarInfo[] new_vars);

  /**
   * @return array of the VarInfos this was derived from
   **/
  public abstract VarInfo[] getBases();

  /**
   * @return a pair of: the derived value and whether the variable
   * counts as modified.
   * @param full_vt The set of values in a program point that will be
   * used to derive the value.
   **/
  // I don't provide separate computeModified and computeValue
  // functions: they aren't so useful, and the same computation must
  // usually be done in both functions.
  // A value whose derivation doesn't make sense is considered
  // MISSING_NONSENSICAL, not MISSING_FLOW.
  public abstract ValueAndModified computeValueAndModified(ValueTuple full_vt);

  /**
   * Get the VarInfo that this would represent.  However,
   * the VarInfo can't be used to obtain values without further
   * modification -- use computeValueAndModified() for this.
   * @see Derivation#computeValueAndModified
   **/
  public VarInfo getVarInfo() {
    if (this_var_info == null) {
      this_var_info = makeVarInfo();
      makeVarInfo_common_setup(this_var_info);
    }
    return this_var_info;
  }
  private VarInfo this_var_info;

  /**
   * Used by all child classes to actually create the VarInfo this
   * represents, after which it is interned for getVarInfo().
   **/
  // This is in each class, but I can't have a private abstract method.
  protected abstract VarInfo makeVarInfo();

  protected void makeVarInfo_common_setup(VarInfo vi) {
    // Common tasks that are abstracted into here.
    vi.derived = this;
    vi.canBeMissing = canBeMissing();
    if (isParam()) {
      this_var_info.set_is_param();
      // VIN
      // this_var_info.aux = vi.aux.setValue(VarInfoAux.IS_PARAM,
      //                                    VarInfoAux.TRUE);
    }
  }

  // Set whether the derivation is a param according to aux info
  protected abstract boolean isParam();

  public boolean missing_array_bounds = false;
  /**
   * True if we have encountered to date any missing values in this
   * derivation due to array indices being out of bounds.  This can
   * happen with both simple subscripts and subsequences.  Note that
   * this becomes true as we are running, it cannot be set in advance
   * (which would require a first pass).
   **/
  public boolean missingOutOfBounds() {
    return (missing_array_bounds);
  }

  /* *
   * For debugging only; returns true if the variables from which this
   * one was derived are all non-canonical (which makes this derived
   * variable uninteresting).  We might not have been able to know
   * before performing the derivation that this would be the case --
   * for instance, when deriving before any values are seen.
   **/
  public abstract boolean isDerivedFromNonCanonical();

  /**
   * Returns how many levels of derivation this Derivation is based
   * on.  The depth counts this as well as the depths of its bases.
   **/
  public abstract int derivedDepth();

  /**
   * @return true iff other and this represent the same derivation
   * (modulo the variable they are applied to).  Default implentation
   * will just checks runtime type, but subclasses with state
   * (e.g. SequenceInitial index) should match that, too.
   **/
  public abstract boolean isSameFormula(Derivation other);

  public abstract boolean canBeMissing();

  /**
   * Returns the lower bound of a slice.  Throws an error if this is not
   * a slice.  Slices should override.
   */
  public Quantify.Term get_lower_bound() {
    throw new RuntimeException ("not a slice derivation: " + this);
  }

  /**
   * Returns the lower bound of a slice.  Throws an error if this is not
   * a slice.  Slices should override.
   */
  public Quantify.Term get_upper_bound() {
    throw new RuntimeException ("not a slice derivation: " + this);
  }

  /**
   * Returns the array variable that underlies this slice.  Throws an error
   * if this is not a slice.  Slices should override.
   */
  public VarInfo get_array_var() {
    throw new RuntimeException ("not a slice derivation: " + this);
  }

  /**
   * Returns the name of this variable in ESC format.  If an index
   * is specified, it is used as an array index.  It is an error to
   * specify an index on a non-array variable
   */
  public String esc_name (String index) {
    throw new RuntimeException ("esc_name not implemented for " + this);
  }

  /**
   * Returns the name of this variable in JML format.  If an index
   * is specified, it is used as an array index.  It is an error to
   * specify an index on a non-array variable
   */
  public String jml_name (String index) {
    return esc_name (index);
  }

  /** Returns the name of this variable in simplify format **/
  public String simplify_name() {
    throw new RuntimeException ("simplify_name not implemented for "
                                + this.getClass() + " (" + this + ")");
  }

  /**
   * Returns true if d is the prestate version of this.  Returns true
   * if this and d are of the same derivation with the same formula
   * and have the same bases.
   */
  public boolean is_prestate_version (Derivation d) {

    // The derivations must be of the same type
    if (getClass() != d.getClass())
        return false;

    // Each base of vi must be the prestate version of this
    VarInfo[] base1 = getBases();
    VarInfo[] base2 = d.getBases();
    for (int ii = 0; ii < base1.length; ii++) {
      if (!base1[ii].is_prestate_version(base2[ii]))
        return false;
    }

    // The derivations must have the same formula (offset, start_from, etc)
    return isSameFormula (d);
  }

  /**
   * Return the complexity of this derivation.  This is only for the
   * derivation itself and not for the variables included in the derivation.
   * The default implementation returns 1 (which is the added complexity of
   * an derivation).  Subclasses that add additional complexity (such as an
   * offset) should override
   */
  public int complexity() {
    return 1;
  }

  /** Returns a string that corresponds to the the specified shift **/
  protected String shift_str (int shift) {
    String shift_str = "";
    if (shift != 0)
      shift_str = String.format ("%+d", shift);
    return (shift_str);
  }

  /**
   * Returns the esc name of a variable which is included inside
   * an an expression (such as orig(a[vi])).  If the expression
   * is orig, the orig is implied for this variable.
   */
  protected String inside_esc_name (VarInfo vi, boolean in_orig, int shift) {
    if (vi == null)
      return "";

    if (in_orig) {
      if (vi.isPrestate())
        return vi.postState.esc_name() + shift_str(shift);
      else
        return String.format ("\\new(%s)%s", vi.esc_name(), shift_str(shift));
    } else
      return vi.esc_name() + shift_str(shift);
  }

  /**
   * Returns the jml name of a variable which is included inside
   * an an expression (such as orig(a[vi])).  If the expression
   * is orig, the orig is implied for this variable.
   */
  protected String inside_jml_name (VarInfo vi, boolean in_orig, int shift) {
    if (vi == null)
      return "";

    if (in_orig) {
      if (vi.isPrestate())
        return vi.postState.jml_name() + shift_str(shift);
      else
        return String.format ("\\new(%s)%s", vi.jml_name(), shift_str(shift));
    } else
      return vi.jml_name() + shift_str(shift);
  }

}
