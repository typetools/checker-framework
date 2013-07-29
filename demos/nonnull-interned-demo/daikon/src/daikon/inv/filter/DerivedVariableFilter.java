package daikon.inv.filter;

import java.util.regex.Pattern;
import daikon.inv.*;
import daikon.VarInfo;

/**
 * A filter that filters out invariants that contain derived variables
 * of a specified derivation.  If the derivation class name contains
 * the regular expression in dkconfig_class_re, the invariant is filtered
 * out.  By default, no derived variables are matched.
 */
public class DerivedVariableFilter extends InvariantFilter {

  public String getDescription() {
    return "Derived Variable filter on '" + dkconfig_class_re + "'";
  }

  /**
   * Regular expression to match against the class name of derived
   * variables.  Invariants that contain derived variables that match will
   * be filtered out.  If null, nothing will be filtered out.
   **/
  public static String dkconfig_class_re = null;
  public static Pattern class_re = null;

  /**
   */
  public DerivedVariableFilter () {
    isOn = dkconfig_class_re != null;
    if (isOn) {
      class_re = Pattern.compile (dkconfig_class_re);
    }
  }

  public String get_derivation_class_re() {
    return dkconfig_class_re;
  }

  boolean shouldDiscardInvariant( Invariant invariant ) {

    for (VarInfo vi : invariant.ppt.var_infos) {
      if (vi.derived == null)
        continue;
      // System.out.printf ("Comparing %s to %s\n",
      //                   vi.derived.getClass().getName(), class_re);
      if (class_re.matcher (vi.derived.getClass().getName()).find())
        return (true);
    }
    return (false);
  }
}
