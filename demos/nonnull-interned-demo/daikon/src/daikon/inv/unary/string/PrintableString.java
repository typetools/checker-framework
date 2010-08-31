package daikon.inv.unary.string;

import daikon.*;
import daikon.inv.*;

import utilMDE.*;

import java.util.*;

/**
 * Represents a string that contains only printable ascii characters
 * (values 32 through 126 plus 9 (tab)
 */
public final class PrintableString extends SingleString
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20061016L;

  /**
   * Boolean.  True iff PrintableString invariants should be considered.
   **/
  public static boolean dkconfig_enabled = false;

  public PrintableString (PptSlice slice) {
    super (slice);
    if (slice == null)
      return;
  }

  private static PrintableString proto;

  /** Returns the prototype invariant for PrintableString **/
  public static Invariant get_proto() {
    if (proto == null)
      proto = new PrintableString (null);
    return (proto);
  }

  /** returns whether or not this invariant is enabled **/
  public boolean enabled() {
    return dkconfig_enabled;
  }

  /** instantiate an invariant on the specified slice **/
  public Invariant instantiate_dyn (PptSlice slice) {
    return new PrintableString(slice);
  }


  /** return description of invariant.  Only Daikon format is implemented **/
  public String format_using(OutputFormat format) {
    if (format == OutputFormat.DAIKON)
      return var().name() + " is printable";
    else
      return format_unimplemented (format);
  }

  /** Check to see if a only contains printable ascii characters **/
  public InvariantStatus add_modified(String a, int count) {
    return check_modified (a, count);
  }

  /** Check to see if a only contains printable ascii characters **/
  public InvariantStatus check_modified(String a, int count) {
    for (int ii = 0; ii < a.length(); ii++) {
      char ch = a.charAt(ii);
      if (ch > 126)
        return InvariantStatus.FALSIFIED;
      if ((ch < 32) && (ch != 9))
        return InvariantStatus.FALSIFIED;
    }
    return InvariantStatus.NO_CHANGE;
  }
  protected double computeConfidence() {
    ValueSet vs = ppt.var_infos[0].get_value_set();
    if (vs.size() > 1)
      return Invariant.CONFIDENCE_JUSTIFIED;
    else
      return Invariant.CONFIDENCE_UNJUSTIFIED;
  }

  /**
   * Returns whether or not this is obvious statically.  The only check
   * is for static constants which are obviously printable (or not)
   * from their values
   */
  public DiscardInfo isObviousStatically(VarInfo[] vis) {
    if (vis[0].isStaticConstant()) {
      return new DiscardInfo(this, DiscardCode.obvious, vis[0].name()
                             + " is a static constant.");
    }
    return super.isObviousStatically(vis);
  }

  public boolean isSameFormula(Invariant o) {
    assert o instanceof PrintableString;
    return true;
  }


}
