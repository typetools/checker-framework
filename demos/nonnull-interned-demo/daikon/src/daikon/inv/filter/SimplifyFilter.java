package daikon.inv.filter;

import daikon.PptTopLevel;
import daikon.Daikon;
import daikon.inv.*;

public class SimplifyFilter extends InvariantFilter {
  static String description = "Eliminate invariants based on Simplify (slow)";

  public String getDescription() {
    return description;
  }

  /**
   * Boolean. If true, SimplifyFilter is initially turned on. 
   */
  public static boolean dkconfig_enabled = true;

  public SimplifyFilter( InvariantFilters filters ) {
    isOn = dkconfig_enabled;
  }

  boolean shouldDiscardInvariant( Invariant invariant ) {
    if (Daikon.suppress_redundant_invariants_with_simplify &&
        invariant.ppt.parent.redundant_invs.contains(invariant)) {
      return (true);
    }
    return false;
  }

}
