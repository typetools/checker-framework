package daikon;

import daikon.inv.*;

import java.util.*;
import utilMDE.Assert;

public final class DiscReasonMap {

  // The keys are PptTopLevel name strings, and the values
  // are HashMaps whose keys are Strings containing variable
  // names separated by commas, e.g. "this.head, this.tail" (names appear in sorted order)
  // The values of those HashMaps are lists containing DiscardInfo's
  // for all Invariants using those variable names in that PptTopLevel.
  private static HashMap<String,HashMap<String,List<DiscardInfo>>> the_map;

  private DiscReasonMap() {
    // Use initialize();
  }

  public static void initialize() {
    the_map = new HashMap<String,HashMap<String,List<DiscardInfo>>>();
  }

  /**
   * Adds disc_info to DiscReasonMap's internal data structure, unless
   * a reason already exists for inv, in which case the old reason is kept and
   * disc_info is discarded.
   * Requires: inv != null && disc_info != null && disc_info.shouldDiscard()
   */
  public static void put(Invariant inv, DiscardInfo disc_info) {
    if (! PrintInvariants.print_discarded_invariants)
      return;
    Assert.assertTrue(disc_info != null);

    // Let's not keep track of DiscardInfo's from Invariants who have
    // any repeated variables since we don't expect them to print anyway
    for (int i = 1; i < inv.ppt.var_infos.length; i++) {
      if (inv.ppt.var_infos[i] == inv.ppt.var_infos[i-1])
        return;
    }

    String vars_result = inv.ppt.var_infos[0].name();
    for (int i = 1; i < inv.ppt.var_infos.length; i++) {
      vars_result += "," + (inv.ppt.var_infos[i]).name();
    }
    put(vars_result, inv.ppt.parent.name, disc_info);
  }

  public static void put(Invariant inv, DiscardCode discardCode, String discardString) {
    if (! PrintInvariants.print_discarded_invariants)
      return;
    put(inv, new DiscardInfo(inv, discardCode, discardString));
  }

  public static void put(String vars, String ppt, DiscardInfo disc_info) {
    if (! PrintInvariants.print_discarded_invariants)
      return;
    Assert.assertTrue(disc_info != null);

    // Get the vars out of inv in our proper format
    // I should move this var_sorting stuff to a central
    // place soon since it's being used kind of frequently
    /*StringTokenizer st = new StringTokenizer(vars, ",");
    ArrayList<String> temp_vars = new ArrayList();
    while (st.hasMoreTokens()) {
      temp_vars.add(st.nextToken());
    }
    Collections.sort(temp_vars);
    String vars_result = "";
    for (String temp_var : temp_vars) {
      vars_result += temp_var + ",";
      }*/

    HashMap<String,List<DiscardInfo>> ppt_hashmap = the_map.get(ppt);
    if (ppt_hashmap != null) {
      List<DiscardInfo> disc_infos = ppt_hashmap.get(vars);
      if (disc_infos != null) {
        // Check to see if this invariant already has a DiscInfo
        for (DiscardInfo di : disc_infos) {
          if (disc_info.className().equals(di.className())) {
            // We already have a reason for why the Invariant was discarded
            // Perhaps we could replace it with the new reason, but maybe we
            // want to be able to "default" to reasons.  i.e., set less specific
            // reasons at the very end for everything not printed, just in case
            // some discarded invariants haven't had their reasons set yet
            return;
          }
        }
        disc_infos.add(disc_info);
      } else {
        List<DiscardInfo> temp = new ArrayList<DiscardInfo>();
        temp.add(disc_info);
        ppt_hashmap.put(vars, temp);
      }
    } else {
      // In case where nothing from this inv's PptTopLevel has been discarded yet
      HashMap<String,List<DiscardInfo>> new_map = new HashMap<String,List<DiscardInfo>>();
      List<DiscardInfo> temp = new ArrayList<DiscardInfo>();
      temp.add(disc_info);
      new_map.put(vars, temp);
      the_map.put(ppt, new_map);
    }
  }

  /**
   * Requires: vars is given in the form "var1,var2,var3" in ascending
   * alphabetical order with no spaces && invInfo.ppt() != null.
   * @return a List of all DiscardInfos di such that && the di is for an
   * Invariant from discPpt whose class and vars match the params passed
   * into the method call. If the user wishes for any of the 3 params to be
   * a wildcard, they can pass that/those param(s) in as null.
   **/
  public static List<DiscardInfo> returnMatches_from_ppt(InvariantInfo invInfo) {
    ArrayList<DiscardInfo> result = new ArrayList<DiscardInfo>();
    HashMap<String,List<DiscardInfo>> vars_map_from_ppt = the_map.get(invInfo.ppt());

    if (vars_map_from_ppt == null) {
      return result;
    }

    List<DiscardInfo> di_list = new ArrayList<DiscardInfo>();
    if (invInfo.vars() != null) {
      // The user entered the vars in a specific order, but let's give
      // them matching invariants that have those vars in any order
      List<String> var_perms = invInfo.var_permutations();
      for (String var_perm : var_perms) {
        List<DiscardInfo> temp = vars_map_from_ppt.get(var_perm);
        if (temp != null) {
          di_list.addAll(temp);
        }
      }
    } else {
      di_list = all_vars_tied_from_ppt(invInfo.ppt());
    }

    for (DiscardInfo di : di_list) {
      // Assert.assertTrue(di.discardCode() != DiscardCode.not_discarded);
      String shortName = di.className().substring(di.className().lastIndexOf('.')+1); // chop off hierarchical info
      if ((invInfo.className() == null) || invInfo.className().equals(di.className())
          || invInfo.className().equals(shortName)) {
        result.add(di);
      }
    }
    return result;
  }

  // Helper function used to combine all the DiscardInfo lists associated
  // with a set of vars at a ppt.  Only called when we know ppt has at
  // least 1 DiscardInfo associated with it
  private static List<DiscardInfo> all_vars_tied_from_ppt(String ppt) {
    HashMap<String,List<DiscardInfo>> vars_map = the_map.get(ppt);
    Assert.assertTrue(vars_map != null);

    ArrayList<DiscardInfo> result = new ArrayList<DiscardInfo>();
    for (List<DiscardInfo> ldi : vars_map.values()) {
      result.addAll(ldi);
    }
    return result;
  }

  /**
   * Prints out all vars from ppt that have DiscardInfo's in the
   * Set.toString() format.
   */
  public static void debugVarMap(String ppt) {
    System.out.println();
    System.out.println();
    System.out.println("DEBUGGING PPT: " + ppt);
    HashMap<String,List<DiscardInfo>> vars_map = the_map.get(ppt);
    if (vars_map == null) {
      System.out.println("No reasons for this ppt");
      return;
    }
    System.out.println(vars_map.keySet().toString());
  }

}
