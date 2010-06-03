package daikon.inv.filter;

import daikon.*;
import daikon.inv.*;
import daikon.inv.unary.scalar.*;

public class OnlyConstantVariablesFilter extends InvariantFilter {
  public String getDescription() {
    return "Suppress invariants containing only constants";
  }

  /**
   * Boolean. If true, OnlyConstantVariablesFilter is initially turned on. 
   */
  public static boolean dkconfig_enabled = true;

  public OnlyConstantVariablesFilter () {
    isOn = dkconfig_enabled;
  }


  boolean shouldDiscardInvariant( Invariant invariant ) {
    // System.out.println("OnlyConstantVariablesFilter: " + invariant.format());
    if (IsEqualityComparison.it.accept(invariant)) {
      return false;
    }
    if (invariant instanceof OneOf) {
      return false;
    }
    if (invariant instanceof Implication) {
      Implication impl = (Implication) invariant;
      // jhp 11/04/03, comment out if below.  It seems to never make sense
      // to look at constant variables for the implication itself since
      // the implication is in a PptSlice0 (which will always fail the test
      // below
      // if (impl.predicate().isGuardingPredicate)
        // only consider the consequent
        invariant = impl.consequent();

    }

    VarInfo[] vis = invariant.ppt.var_infos;
    for (int i=0; i<vis.length; i++) {
      if (! isConstant(vis[i])) {
        // System.out.println("In " + invariant.format() + ": non-constant " + vis[i].name);
        return false;
      }
    }
    return true;

  }

  boolean isConstant(VarInfo vi) {
    PptTopLevel ppt = vi.ppt;
    boolean isStaticConstant = vi.is_static_constant;
    boolean isDynamicConstant = ((ppt.constants != null)
                                 && ppt.constants.is_constant(vi));
    PptSlice view = ppt.findSlice(vi);
    // TODO: This should be generalized to other types of scalar
    OneOfScalar oos = (view == null) ? null : OneOfScalar.find(view);
    OneOfFloat oof = (view == null) ? null : OneOfFloat.find(view);
    boolean isOneOfConstant = (((oos != null)
                                && (oos.num_elts() == 1)
                                && (! oos.is_hashcode()))
                               ||
                               ((oof != null)
                                && (oof.num_elts() == 1)
                                // no hashcode test for floats
                                // && (! oof.is_hashcode())
                                ));
    return isStaticConstant || isDynamicConstant || isOneOfConstant;
  }

}
