package daikon.inv.unary.scalar;

import daikon.*;
import daikon.inv.OutputFormat;
import daikon.inv.Invariant;
import daikon.inv.InvariantStatus;

// This invariant is true if the variable is always positive (greater than 0).
// This invariant is provided for pedagogical reasons only.

/**
 * Represents the invariant <samp>x &gt; 0</samp> where <samp>x</samp>
 * is a long scalar.  This exists
 * only as an example for the purposes of the manual.  It isn't actually
 * used (it is replaced by the more general invariant LowerBound).
 **/

public class Positive
  extends SingleScalar
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20040728L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff Positive invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  ///
  /// Required methods
  ///

  private Positive(PptSlice ppt) {
    super(ppt);
  }

  private static Positive proto;

  /** Returns the prototype invariant **/
  public static Invariant get_proto() {
    if (proto == null)
      proto = new Positive (null);
    return (proto);
  }

  /** returns whether or not this invariant is enabled **/
  public boolean enabled() {
    return dkconfig_enabled;
  }

  /** instantiate an invariant on the specified slice **/
  public Invariant instantiate_dyn (PptSlice slice) {
    return new Positive(slice);
  }

  // A printed representation for user output
  public String format_using(OutputFormat format) {
    return var().name() + " > 0";
  }

  public InvariantStatus check_modified(long v, int count) {
    if (v <= 0) {
      return InvariantStatus.FALSIFIED;
    }
    return InvariantStatus.NO_CHANGE;
  }

  public InvariantStatus add_modified(long v, int count) {
    return check_modified(v, count);
  }

  protected double computeConfidence() {
    // Assume that every variable has a .5 chance of being positive by
    // chance.  Then a set of n values have a have (.5)^n chance of all
    // being positive by chance.
    return 1 - Math.pow(.5, ppt.num_samples());
  }

}
