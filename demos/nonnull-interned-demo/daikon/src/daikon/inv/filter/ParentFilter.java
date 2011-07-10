package daikon.inv.filter;

import daikon.inv.*;
import daikon.*;
import java.util.*;

/**
 * Filter for not printing invariants that have a matching invariant
 * at their parent PPT.
 **/
public class ParentFilter extends InvariantFilter {
  public String getDescription() {
    return "Filter invariants that match a parent program point invariant";
  }

  /**
   * Boolean. If true, ParentFilter is initially turned on.
   */
  public static boolean dkconfig_enabled = true;

  public ParentFilter () {
    isOn = dkconfig_enabled;
  }

  private static boolean debug = false;
  // private static boolean debug = true;

  boolean shouldDiscardInvariant( Invariant inv ) {

    // System.out.printf("shouldDiscardInvariant(%s)%n", inv.format());

    if (Debug.logDetail()) {
      if (inv.ppt.parent.parents != null) {
        inv.log ("%s has PptTopLevel %s which has %d parents",
            inv.format(), inv.ppt.parent.name, inv.ppt.parent.parents.size());
        for (PptRelation rel : inv.ppt.parent.parents) {
          inv.log ("--%s%n", rel);
          inv.log ("--variables: %s", VarInfo.toString (rel.parent.var_infos));
          inv.log ("--map: %s", rel.child_to_parent_map);
        }
      } else {
        inv.log ("%s has PptTopLevel %s which has 0 parents", inv.format(),
                 inv.ppt.parent.name);
      }
    }

    // If there are no parents, can't discard
    if (inv.ppt.parent.parents == null)
      return (false);

    // Loop through each parent ppt getting the parent/child relation info
    outer: for (PptRelation rel : inv.ppt.parent.parents) {

      if (Debug.logDetail())
        inv.log ("  considering parent %s [%s]", rel, rel.parent.name());

      // Look up each variable in the parent, skip this parent if any
      // variables don't exist in the parent.
      VarInfo[] pvis = new VarInfo[inv.ppt.var_infos.length];
      for (int j = 0; j < pvis.length; j++) {
        pvis[j] = rel.parentVar (inv.ppt.var_infos[j]);
        if (pvis[j] == null) {
          if (Debug.logDetail()) {
            inv.log ("variable %s [%s] cannot be found in %s",
                     inv.ppt.var_infos[j],
                     inv.ppt.var_infos[j].get_equalitySet_vars(), rel);
            for (VarInfo evi : inv.ppt.var_infos[j].get_equalitySet_vars())
              inv.log ("var %s index %d, dp %b, depth %d, complex %d, idp %s, name %s, param vars %s",
                       evi, evi.varinfo_index, evi.isDerivedParamAndUninteresting(), evi.derivedDepth(), evi.complexity(), evi.isDerivedParam(), evi.get_VarInfoName(), evi.ppt.getParamVars());
          }
          continue outer;
        }
      }

      if (Debug.logDetail())
        inv.log ("  got variables");

      // Sort the parent variables in index order
      Arrays.sort (pvis, VarInfo.IndexComparator.getInstance());
      if (Debug.logDetail())
        inv.log ("Found parent vars: " + VarInfo.toString (pvis));

      // Lookup the slice, skip if not found
      PptSlice pslice = rel.parent.findSlice (pvis);
      if (pslice == null)
        continue;
      if (Debug.logDetail())
        inv.log ("Found parent slice: " + pslice.name());

      // System.out.printf ("  found parent slice (%d invs): %s%n", pslice.invs.size(), pslice.name());

      // Look for a matching invariant in the parent slice.  Don't filter out
      // NonZero invariants if the parent invariant is 'this != null' since it
      // it is not obvious to the user that 'this != null' implies that all
      // references to the class are non null
      for (Invariant pinv : pslice.invs) {
        // System.out.printf ("  inv in parent slice: %s%n", pinv.format());
        if (pinv.isGuardingPredicate)
          continue;
        if (pinv.getClass() != inv.getClass())
          continue;
        if ((pinv.getClass() == daikon.inv.unary.scalar.NonZero.class)
            && pinv.ppt.var_infos[0].isThis()) {
          inv.log ("Not filtered by " + pinv.format() + " 'this != null'");
          continue;
        }
        if (! pinv.isSameFormula (inv))
          continue;

        // Check that all the guard variables correspond.
        List<VarInfo> guardedVars = inv.getGuardingList();
        List<VarInfo> pGuardedVars = pinv.getGuardingList();
        // Optimization: bail our early if size of list is different.
        if ((guardedVars.size() != pGuardedVars.size())
            && (guardedVars.size() != pGuardedVars.size()+1))
          continue;
        boolean var_mismatch = false;
        for (VarInfo v : guardedVars) {
          VarInfo pv = rel.parentVarAnyInEquality(v);
          // VarInfo pv = rel.parentVar(v);
          if (pv == null) {
            if (debug) {
              System.out.printf("    ParentFilter %s, parent %s%n", inv.format(), pslice.name());
              System.out.printf("    No parent var for %s via %s%n", v.name(), rel);
              System.out.printf("      Equality set: %s%n", v.equalitySet.shortString());
            }
            var_mismatch = true;
            break;
          }
          if (! (pv.name().equals("this")
                 || pGuardedVars.contains(pv))) {
            if (debug) System.out.printf("Not in guarding list %s for %s: parent var %s at %s for %s at %s%n",
                              guardedVars, pinv, pv, rel.parent, v.name(), rel.child);
            VarInfo pgv = pGuardedVars.get(0);
            assert (pgv != pv);
            if (debug) System.out.printf("%s is index %d at %s, %s is index %d at %s%n",
                              pgv, pgv.varinfo_index, pgv.ppt.name, pv, pv.varinfo_index, pv.ppt.name);
            var_mismatch = true;
            break;
          }
        }
        if (var_mismatch)
          continue;

        if (inv.logOn()) {
          inv.log ("Filtered by parent inv '%s' at ppt %s with rel %s",
                   pinv.format(), pslice.name(), rel);
          for (VarInfo cvi : inv.ppt.var_infos) {
            inv.log ("child variable %s matches parent variable %s",
                     cvi, rel.parentVar (cvi));
            for (VarInfo evi : cvi.get_equalitySet_vars())
              inv.log ("var %s index %d, dp %b, depth %d, complex %d",
                       evi, evi.varinfo_index, evi.isDerivedParamAndUninteresting(), evi.derivedDepth(), evi.complexity());
          }
        }
        return (true);
      }
    }

    return (false);
  }

}
