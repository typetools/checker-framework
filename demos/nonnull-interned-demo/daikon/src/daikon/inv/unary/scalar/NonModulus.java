package daikon.inv.unary.scalar;

import daikon.*;
import daikon.inv.*;
import utilMDE.*;
import java.util.*;

/**
 * Represents long scalars that are never equal to <code>r (mod m)</code>
 * where all other numbers in the same range (i.e., all the values that
 * <code>x</code> doesn't take from <code>min(x)</code> to
 * <code>max(x)</code>) are equal to <code>r (mod m)</code>.
 * Prints as <samp>x != r (mod m)</samp>, where <samp>r</samp>
 * is the remainder and <samp>m</samp> is the modulus.
 **/

public class NonModulus
  extends SingleScalar
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff NonModulus invariants should be considered.
   **/
  public static boolean dkconfig_enabled = false;

  // Set elements = new HashSet();
  SortedSet<Long> elements = new TreeSet<Long>();

  private long modulus = 0;
  private long remainder = 0;
  // The next two variables indicate whether the "modulus" and "result"
  // fields are up to date.
  // Indicates that no nonmodulus has been found; maybe with more
  // samples, one will appear.
  private boolean no_result_yet = false;
  // We don't continuously keep the modulus and remainder field up to date.
  // This indicates whether it is.
  private boolean results_accurate = false;

  private NonModulus(PptSlice ppt) {
    super(ppt);
  }

  private static NonModulus proto;

  /** Returns the prototype invariant for NonModulus **/
  public static Invariant get_proto() {
    if (proto == null)
      proto = new NonModulus (null);
    return (proto);
  }

  /** NonModulus is only valid on integral types **/
  public boolean instantiate_ok (VarInfo[] vis) {

    if (!valid_types (vis))
      return (false);

    return (vis[0].file_rep_type.baseIsIntegral());
  }

  /** Returns whether or not this invariant is enabled **/
  public boolean enabled() {
    return dkconfig_enabled;
  }

  /** instantiate an invariant on the specified slice **/
  protected Invariant instantiate_dyn (PptSlice slice) {
    return new NonModulus (slice);
  }

  public NonModulus clone() {
    NonModulus result = (NonModulus) super.clone();
    result.elements = new TreeSet<Long>(this.elements);
    return result;
  }

  public String repr() {
    return "NonModulus" + varNames() + ": "
      + "m=" + modulus + ",r=" + remainder;
  }

  public String format_using(OutputFormat format) {
    updateResults();
    String name = var().name_using(format);

    if (format == OutputFormat.DAIKON) {
      if (no_result_yet) {
        return name + " != ? (mod ?) ***";
      }
      return name + " != " + remainder + "  (mod " + modulus + ")";
    }

    if (no_result_yet) {
      return format_too_few_samples(format, null);
    }

    if (format == OutputFormat.IOA) {
      return "mod(" + name + ", " + modulus + ") ~= " + remainder;
    }

    if (format.isJavaFamily()) {

      if (var().type.isFloat()) {
        return "daikon.Quant.fuzzy.ne(" + name + " % " + modulus + ", " + remainder + ")";
      } else {
        return name + " % " + modulus + " != " + remainder;
      }
    }

    if (format == OutputFormat.SIMPLIFY) {
      return "(NEQ (MOD " + var().simplify_name() + " "
        + simplify_format_long(modulus) + ") "
        + simplify_format_long(remainder) + ")";
    }

    return format_unimplemented(format);
  }

  // Set either modulus and remainder, or no_result_yet.
  void updateResults() {
    if (results_accurate)
      return;
    if (elements.size() == 0) {
      no_result_yet = true;
    } else {
      // Do I want to communicate back some information about the smallest
      // possible modulus?
      long[] result = MathMDE.nonmodulus_strict_long(elements.iterator());
      if (result == null) {
        no_result_yet = true;
      } else {
        remainder = result[0];
        modulus = result[1];
        no_result_yet = false;
      }
    }
    results_accurate = true;
  }

  public InvariantStatus check_modified(long value, int count) {
    return InvariantStatus.NO_CHANGE;
  }

  // XXX have to deal with flowing this; maybe it should live at all ppts?
  public InvariantStatus add_modified(long value, int count) {
    if (elements.add(Intern.internedLong(value))
        && results_accurate
        && (! no_result_yet)
        && (MathMDE.mod_positive(value, modulus) == remainder))
      results_accurate = false;
    return InvariantStatus.NO_CHANGE;
  }

  protected double computeConfidence() {
    updateResults();
    if (no_result_yet)
      return Invariant.CONFIDENCE_UNJUSTIFIED;
    double probability_one_elt_nonmodulus = 1 - 1.0/modulus;
    // return 1 - Math.pow(probability_one_elt_nonmodulus, ppt.num_mod_samples());
    return 1 - Math.pow(probability_one_elt_nonmodulus, ppt.num_samples());
  }

  public boolean isSameFormula(Invariant o) {
    NonModulus other = (NonModulus) o;

    updateResults();
    other.updateResults();

    if (no_result_yet && other.no_result_yet) {
      return true;
    } else if (no_result_yet || other.no_result_yet) {
      return false;
    } else {
      return
        (modulus == other.modulus) &&
        (remainder == other.remainder);
    }
  }

  /** Returns true if this has the given modulus and remainder. **/
  public boolean hasModulusRemainder(long modulus, long remainder) {
    updateResults();
    if (no_result_yet)
      return false;

    return ((modulus == this.modulus)
            && (remainder == this.remainder));
  }


  public boolean isExclusiveFormula(Invariant o) {
    updateResults();
    if (no_result_yet)
      return false;
    if (o instanceof NonModulus) {
      NonModulus other = (NonModulus) o;
      other.updateResults();
      if (other.no_result_yet)
        return false;
      return ((modulus == other.modulus)
              && (remainder != other.remainder));
    } else if (o instanceof Modulus) {
      Modulus other = (Modulus) o;
      return ((modulus == other.modulus)
              && (remainder == other.remainder));
    }

    return false;
  }

}
