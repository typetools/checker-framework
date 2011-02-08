package daikon.inv.filter;

import daikon.inv.*;

public class ObviousFilter extends InvariantFilter {
  public String getDescription() { return "Suppress obvious invariants"; }

  /**
   * Boolean. If true, ObviousFilter is initially turned on. 
   */
  public static boolean dkconfig_enabled = true;

  public ObviousFilter () {
    isOn = dkconfig_enabled;
  }


  boolean shouldDiscardInvariant( Invariant invariant ) {
    // if ((invariant.ppt.arity() == 1) || IsEqualityComparison.it.accept(invariant)) {
    DiscardInfo discard = invariant.isObvious();
    if (discard != null)
      invariant.log ("discard because " + discard.discardString());
    return (discard != null);
      /* }
    else {
      // if y cmp f(x_0,x_1, ..., x_n) and x_n is a constant, then we can
      // write y cmp f'(x_0,x_1,...,x_n-1), so we loop through the var_infos
      // array of invariant and return true if any var is constant since an implying
      // invariant should appear with some function over fewer variables.
      for (int i=0; i < invariant.ppt.var_infos.length; i++) {
        VarInfo var = invariant.ppt.var_infos[i];
        PptSlice slice = invariant.ppt.parent.findSlice(var);
        if (slice != null) {
          Invariant oo = null;
          int num_elts = -1;
          if (var.type.isIntegral()) {
            oo = OneOfScalar.find(slice);
            if (oo != null)
              num_elts = ((OneOfScalar) oo).num_elts();
          } else if (var.type.isFloat()) {
            oo = OneOfFloat.find(slice);
            if (oo != null)
              num_elts = ((OneOfFloat) oo).num_elts();
          } else if (var.type.baseIsIntegral()) {
            oo = EltOneOf.find(slice);
            if (oo != null)
              num_elts = ((EltOneOf) oo).num_elts();
          } else if (var.type.baseIsFloat()) {
              oo = EltOneOfFloat.find(slice);
              if (oo != null)
                num_elts = ((EltOneOfFloat) oo).num_elts();
          }
          if ((oo != null) && (num_elts == 1)) {
            invariant.discardCode = DiscardCode.obvious;
            invariant.discardString = "Variable " + var.name.name() + "is a constant";
            return true;
          }
        }
      }
      return invariant.isObvious();
      }*/
  }
}
