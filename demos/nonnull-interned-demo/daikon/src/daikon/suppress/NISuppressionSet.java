package daikon.suppress;

import daikon.*;
import daikon.inv.*;
import daikon.inv.binary.*;
import utilMDE.*;

import java.lang.reflect.*;
import java.util.logging.*;
import java.util.*;

/**
 * Class that defines a set of non-instantiating suppressions for a single
 * invariant (suppressee).
 */
public class NISuppressionSet implements Iterable<NISuppression> {

  public static final Logger debug
        = Logger.getLogger ("daikon.suppress.NISuppressionSet");

  NISuppression[] suppression_set;

  public NISuppressionSet (NISuppression[] suppressions) {

    suppression_set = suppressions;
  }


  public Iterator<NISuppression> iterator() {
    return (Arrays.asList(suppression_set).iterator());
  }

  /**
   * Adds this set to the suppressor map.  The map is from the class of
   * the suppressor to this. If the same suppressor class appears more
   * than once, the suppression is only added once.
   */
  public void add_to_suppressor_map (Map<Class,List<NISuppressionSet>> suppressor_map) {

    Set<Class> all_suppressors = new LinkedHashSet<Class>();

    // Loop through each suppression in the suppression set
    for (int i = 0; i < suppression_set.length; i++) {
      NISuppression suppression = suppression_set[i];

      // Loop through each suppressor in the suppression
      for (Iterator<NISuppressor> j = suppression.suppressor_iterator(); j.hasNext(); ) {
        NISuppressor suppressor = j.next();

        // If we have seen this suppressor already, skip it
        if (all_suppressors.contains (suppressor.get_inv_class()))
          continue;

        // Note that we have now seen this suppressor invariant class
        all_suppressors.add (suppressor.get_inv_class());


        // Get the list of suppression sets for this suppressor.  Create it
        // if this is the first one.  Add this set to the list
        List<NISuppressionSet> suppression_set_list
                     = suppressor_map.get (suppressor.get_inv_class());
        if (suppression_set_list == null) {
          suppression_set_list = new ArrayList<NISuppressionSet>();
          suppressor_map.put (suppressor.get_inv_class(),
                              suppression_set_list);
        }
        suppression_set_list.add (this);
      }
    }
  }

  /**
   * NIS process a falsified invariant. This method should be called for
   * each falsified invariant in turn.  Any invariants for which inv is
   * the last valid suppressor are added to new_invs.
   *
   * Note, this is no longer the preferred approach, but is kept for
   * informational purposes.  Use NIS.process_falsified_invs() instead.
   */
  public void falsified (Invariant inv, List<Invariant> new_invs) {

    // Get the ppt we are working in
    PptTopLevel ppt = inv.ppt.parent;

    // For now all suppressors are unary/binary and
    // all suppressees are unary, binary or ternary
    Assert.assertTrue (inv.ppt.var_infos.length < 3);

    // check unary, binary and ternary suppressees separately

    // unary suppressee
    if (suppression_set[0].suppressee.var_count == 1) {
      // Create all of the valid unary slices that use the vars from inv
      // and check to see if the invariant should be created for each slice
      if (inv.ppt.var_infos.length == 1) {
        VarInfo[] vis = new VarInfo[1];
        VarInfo v1 = inv.ppt.var_infos[0];
        vis[0] = v1;

        // Make sure the slice is interesting and has valid types over the
        // suppressee invariant
        if (!v1.missingOutOfBounds() && (ppt.is_slice_ok(v1))) {
          if (suppression_set[0].suppressee.sample_inv.valid_types(vis))
            check_falsified(ppt, vis, inv, new_invs);
        }
      }
      return;
    }

    // binary suppressee
    if (suppression_set[0].suppressee.var_count == 2) {
      // Create all of the valid binary slices that use the vars from inv
      // and check to see if the invariant should be created for each slice
      if (inv.ppt.var_infos.length == 2) {
        VarInfo[] vis = new VarInfo[2];
        VarInfo v1 = inv.ppt.var_infos[0];
        VarInfo v2 = inv.ppt.var_infos[1];
        vis[0] = v1;
        vis[1] = v2;

        // Make sure the slice is interesting and has valid types over the
        // suppressee invariant
        if (!v1.missingOutOfBounds() && !v2.missingOutOfBounds() && ppt.is_slice_ok(v1, v2)) {
          if (suppression_set[0].suppressee.sample_inv.valid_types(vis))
            check_falsified(ppt, vis, inv, new_invs);
        }

      } else /* must be unary */{
        VarInfo[] vis = new VarInfo[2];
        VarInfo v1 = inv.ppt.var_infos[0];
        VarInfo[] leaders = ppt.equality_view.get_leaders_sorted();
        for (int i = 0; i < leaders.length; i++) {
          VarInfo l1 = leaders[i];

          // hashcode types are not involved in suppressions
          if (NIS.dkconfig_skip_hashcode_type) {
            if (l1.file_rep_type.isHashcode())
              continue;
          }

          // Make sure the slice is interesting
          if (v1.missingOutOfBounds() || l1.missingOutOfBounds())
            continue;
          if (!ppt.is_slice_ok(v1, l1))
            continue;

          // Sort the variables
          if (v1.varinfo_index <= l1.varinfo_index) {
            vis[0] = v1;
            vis[1] = l1;
          } else {
            vis[0] = l1;
            vis[1] = v1;
          }

          if (!suppression_set[0].suppressee.sample_inv.valid_types(vis))
            continue;

          if (NIS.debug.isLoggable(Level.FINE))
            NIS.debug.fine("processing slice " + Debug.toString(vis) + " in ppt "
                + ppt.name() + " with " + ppt.numViews());

          check_falsified(ppt, vis, inv, new_invs);
        }
      }
      return;
    }


    // ternary suppressee
    if (suppression_set[0].suppressee.var_count == 3) {
      // Create all of the valid ternary slices that use the vars from inv
      // and check to see if the invariant should be created for each slice
      if (inv.ppt.var_infos.length == 2) {
        VarInfo[] vis = new VarInfo[3];
        VarInfo v1 = inv.ppt.var_infos[0];
        VarInfo v2 = inv.ppt.var_infos[1];
        VarInfo[] leaders = ppt.equality_view.get_leaders_sorted();
        for (int i = 0; i < leaders.length; i++) {
          VarInfo l = leaders[i];

          if (NIS.dkconfig_skip_hashcode_type) {
            if (l.file_rep_type.isHashcode())
              continue;
          }

          if (!ppt.is_slice_ok (l, v1, v2))
            continue;
          if (l.missingOutOfBounds() || v1.missingOutOfBounds()
              || v2.missingOutOfBounds())
            continue;

          // Order the variables,
          if (l.varinfo_index <= v1.varinfo_index) {
            vis[0] = l;
            vis[1] = v1;
            vis[2] = v2;
          } else if (l.varinfo_index <= v2.varinfo_index) {
            vis[0] = v1;
            vis[1] = l;
            vis[2] = v2;
          } else {
            vis[0] = v1;
            vis[1] = v2;
            vis[2] = l;
          }

          if (!suppression_set[0].suppressee.sample_inv.valid_types(vis))
            continue;

          if (NIS.debug.isLoggable (Level.FINE))
            NIS.debug.fine ("processing slice " + Debug.toString(vis)
                         + " in ppt " + ppt.name() + " with " + ppt.numViews());

          check_falsified (ppt, vis, inv, new_invs);
        }
      } else /* must be unary */ {
        VarInfo[] vis = new VarInfo[3];
        VarInfo v1 = inv.ppt.var_infos[0];
        VarInfo[] leaders = ppt.equality_view.get_leaders_sorted();
        for (int i = 0; i < leaders.length; i++) {
          VarInfo l1 = leaders[i];

          if (NIS.dkconfig_skip_hashcode_type) {
            if (l1.file_rep_type.isHashcode())
              continue;
          }

          for (int j = i; j < leaders.length; j++) {
            VarInfo l2 = leaders[j];

            if (NIS.dkconfig_skip_hashcode_type) {
              if (l2.file_rep_type.isHashcode())
                continue;
            }

            // Make sure the slice is interesting
            if (v1.missingOutOfBounds() || l1.missingOutOfBounds()
                || l2.missingOutOfBounds())
              continue;
            if (!ppt.is_slice_ok (v1, l1, l2))
              continue;

            // Sort the variables
            if (v1.varinfo_index <= l1.varinfo_index) {
              vis[0] = v1;
              vis[1] = l1;
              vis[2] = l2;
            } else if (v1.varinfo_index <= l2.varinfo_index) {
              vis[0] = l1;
              vis[1] = v1;
              vis[2] = l2;
            } else {
              vis[0] = l1;
              vis[1] = l2;
              vis[2] = v1;
            }

            if (!suppression_set[0].suppressee.sample_inv.valid_types(vis))
              continue;

            if (NIS.debug.isLoggable (Level.FINE))
              NIS.debug.fine ("processing slice " + Debug.toString(vis)
                  + " in ppt " + ppt.name() + " with " + ppt.numViews());

            check_falsified (ppt, vis, inv, new_invs);
          }
        }
      }
      return;
    }
  }

  /**
   * Checks the falsified invariant against the slice specified by vis.
   * If the falsification of inv removed the last valid suppression then
   * instantiates the suppressee
   */
  private void check_falsified (PptTopLevel ppt, VarInfo[] vis, Invariant inv,
                               List<Invariant> new_invs) {

    // process each suppression in the set, marking each suppressor as
    // to whether it is true, false, or matches the falsified inv
    // If any particular suppression is still valid, just return as there
    // is nothing to be done (the suppressee is still suppressed)

    for (int i = 0; i < suppression_set.length; i++ ) {

      String status = suppression_set[i].check (ppt, vis, inv);
      if (status == NIS.VALID) {
        if (NIS.debug.isLoggable (Level.FINE))
          NIS.debug.fine ("suppression " + suppression_set[i] + " is valid");
        return;
      }
      Assert.assertTrue (status != NIS.MISSING);
    }

    if (NIS.debug.isLoggable (Level.FINE))
      NIS.debug.fine ("After check, suppression set: " + this);

    // There are no remaining valid (true) suppressions.  If inv is the
    // first suppressor to be removed from any suppressions, then this
    // falsification removed the last valid suppression.  In that case we
    // need to instantiate the suppressee.
    for (int i = 0; i < suppression_set.length; i++) {
      if (suppression_set[i].invalidated()) {

        Invariant v = suppression_set[i].suppressee.instantiate(vis, ppt);
        if (v != null)
          new_invs.add(v);
        return;
      }
    }
  }

  /**
   * Determines whether or not the suppression set is valid in the
   * specified slice.  The suppression set is valid if any of its
   * suppressions are valid.  A suppression is valid if all of its
   * suppressors are true.
   *
   * @see #is_instantiate_ok(PptSlice) for a check that considers missing
   */
  public boolean suppressed (PptSlice slice) {

    return (suppressed (slice.parent, slice.var_infos));
  }

  /**
   * Determines whether or not the suppression set is valid in the
   * specified ppt and var_infos.  The suppression set is valid if any
   * of its suppressions are valid.  A suppression is valid if all of
   * its suppressors are true.
   *
   * @see #is_instantiate_ok(PptTopLevel,VarInfo[]) for a check that
   * considers missing
   */
  public boolean suppressed (PptTopLevel ppt, VarInfo[] var_infos) {

    // Check each suppression to see if it is valid
    for (int i = 0; i < suppression_set.length; i++ ) {
      String status = suppression_set[i].check (ppt, var_infos, null);
      if (status == NIS.VALID) {
        if (Debug.logOn() || NIS.debug.isLoggable (Level.FINE))
          Debug.log (NIS.debug, getClass(), ppt, var_infos, "suppression "
            + suppression_set[i] + " is " + status + " in ppt " + ppt
            + " with var infos " + VarInfo.toString (var_infos));
        return (true);
      }
    }

    if (Debug.logOn() || NIS.debug.isLoggable (Level.FINE))
      Debug.log (NIS.debug, getClass(), ppt, var_infos, "suppression " + this
                  + " is not valid in ppt " + ppt + " with var infos "
                  + VarInfo.toString (var_infos));
    return (false);
  }

  /**
   * Determines whether or not the suppression set is valid in the
   * specified slice.  The suppression set is valid if any of its
   * suppressions are valid.  A suppression is valid if all of its
   * non-missing suppressors are true.
   */
  public boolean is_instantiate_ok (PptSlice slice) {

    return (is_instantiate_ok (slice.parent, slice.var_infos));
  }

  /**
   * Determines whether or not the suppressee of the suppression set
   * should be instantiated.  Instantiation is ok only if each
   * suppression is invalid.  A suppression is valid if all of
   * its non-missing suppressors are true.
   */
  public boolean is_instantiate_ok (PptTopLevel ppt, VarInfo[] var_infos) {

    // Check each suppression to see if it is valid
    for (int i = 0; i < suppression_set.length; i++ ) {
      String status = suppression_set[i].check (ppt, var_infos, null);
      if ((status == NIS.VALID) || (status == NIS.MISSING)) {
        if (Debug.logOn() || NIS.debug.isLoggable (Level.FINE))
          Debug.log (NIS.debug, getClass(), ppt, var_infos, "suppression "
            + suppression_set[i] + " is " + status + " in ppt " + ppt
            + " with var infos " + VarInfo.toString (var_infos));
        return (false);
      }
    }

    if (Debug.logOn() || NIS.debug.isLoggable (Level.FINE))
      Debug.log (NIS.debug, getClass(), ppt, var_infos, "suppression " + this
                  + " is not valid in ppt " + ppt + " with var infos "
                  + VarInfo.toString (var_infos));
    return (true);
  }

  /**
   * Instantiates the suppressee over the specified variables in the
   * specified ppt.  The invariant is added to the new_invs list, but
   * not to the slice.  The invariant is added to the slice later when
   * the sample is applied to it.  That guarantees that it is only applied
   * the sample once.
   *
   * @deprecated
   */
  @Deprecated
  private void instantiate (PptTopLevel ppt, VarInfo[] vis, List<Invariant> new_invs) {

    NIS.new_invs_cnt++;

    // If the suppressee will be falsified by the sample, don't bother
    // to create it.
    NISuppressee suppressee = suppression_set[0].suppressee;
    // if (suppressee.check (NIS.vt, vis) == InvariantStatus.FALSIFIED) {
    //  NIS.false_invs_cnt++;
    //  return;
    // }

    if (Assert.enabled) {
      for (int i = 0; i < vis.length; i++)
        Assert.assertTrue (!vis[i].missingOutOfBounds());
    }

    // Find the slice and create it if it is not already there.
    // Note that we must make a copy of vis.  vis is used to create each
    // slice and will change after we create the slice which leads to
    // very interesting results.
    PptSlice slice = ppt.findSlice (vis);
    if (slice == null) {
      VarInfo[] newvis = vis.clone();
      slice = new PptSlice3 (ppt, newvis);
      // Fmt.pf ("Adding slice " + slice);
      ppt.addSlice (slice);
    }

    // Create the new invariant
    Invariant inv = suppressee.instantiate (slice);

    if (Debug.logOn() || NIS.debug.isLoggable (Level.FINE))
      inv.log (NIS.debug, "Adding " + inv.format()
               + " from nis suppression set " + this);

    // Make sure the invariant isn't already in the new_invs list
    if (Daikon.dkconfig_internal_check) {
      for (Invariant new_inv : new_invs) {
        if ((new_inv.getClass() == inv.getClass()) && (new_inv.ppt == slice))
          Assert.assertTrue (false, Fmt.spf ("inv %s:%s already in new_invs "
                        + "(slice %s)", inv.getClass(), inv.format(), slice));
      }
    }

    // Add the invariant to the new invariant list
    if (inv != null)
      new_invs.add (inv);

    if (Daikon.dkconfig_internal_check) {
      if (slice.contains_inv_exact (inv)) {
        // Print all unary and binary invariants over the same variables
        for (int i = 0; i < vis.length; i++) {
          PrintInvariants.print_all_invs (ppt, vis[i], "  ");
        }
        PrintInvariants.print_all_invs (ppt, vis[0], vis[1], "  ");
        PrintInvariants.print_all_invs (ppt, vis[1], vis[2], "  ");
        PrintInvariants.print_all_invs (ppt, vis[0], vis[2], "  ");
        Debug.check (Daikon.all_ppts, "assert failure");
        Assert.assertTrue (false, Fmt.spf ("inv %s:%s already in slice %s",
                        inv.getClass(), inv.format(), slice));
      }
    }

  }


  /**
   * Each suppression where a suppressor matches the suppressee in ss is
   * augmented by additional suppression(s) where the suppressor is replaced
   * by each of its suppressions.  This allows recursive suppressions.
   *
   * For example, consider the suppressions:
   *
   *    (r == arg1) && (arg2 <= arg1) ==> r = max(arg1,arg2)
   *    (arg2 == arg1) ==> arg2 <= arg1
   *
   * The suppressor (arg2 <= arg1) in the first suppression matches the
   * suppressee in the second suppression.  In order for the first
   * suppression to work even when (arg2 <= arg1) is suppressed, the
   * second suppression is added to the first:
   *
   *    (r == arg1) && (arg2 <= arg1) ==> r = max(arg1,arg2)
   *    (r == arg1) && (arg2 == arg1) ==> r = max(arg1,arg2)
   *
   * When (arg2 <= arg1) is suppressed, the second suppression for max
   * will still suppress max.  If (arg2 == arg1) is falsified, the
   * (arg2 <= arg1) invariant will be created and can continue to suppress
   * max (as long as it is not falsified itself).
   */
  public void recurse_definitions (NISuppressionSet ss) {

    // Get all of the new suppressions
    List<NISuppression> new_suppressions = new ArrayList<NISuppression>();
    for (int i = 0; i < suppression_set.length; i++) {
      new_suppressions.addAll (suppression_set[i].recurse_definition (ss));
    }
    // This isn't necessarily true if the suppressee is of the same
    // class but doesn't match due to variable swapping.
    // Assert.assertTrue (new_suppressions.size() > 0);

    // Create a new suppression set with all of the suppressions.
    NISuppression[] new_array
      = new NISuppression [suppression_set.length + new_suppressions.size()];
    for (int i = 0; i < suppression_set.length; i++)
      new_array[i] = suppression_set[i];
    for (int i = 0; i < new_suppressions.size(); i++)
      new_array[suppression_set.length + i]
        = new_suppressions.get(i);
    suppression_set = new_array;

  }

  /**
   * Swaps each suppressor and suppressee to the opposite variable
   * order.  Valid only on unary and binary suppressors and suppressees
   */
  public NISuppressionSet swap() {

    NISuppression[] swap_sups = new NISuppression[suppression_set.length];
    for (int i = 0; i < swap_sups.length; i++) {
      NISuppression std_sup = suppression_set[i];
      NISuppressor[] sors = new NISuppressor[std_sup.suppressors.length];
      for (int j = 0; j < sors.length; j++) {
        sors[j] = std_sup.suppressors[j].swap();
      }
      swap_sups[i] = new NISuppression (sors, std_sup.suppressee.swap());
    }
    NISuppressionSet new_ss = new NISuppressionSet (swap_sups);
    // Fmt.pf ("Converted %s to %s", this, new_ss);
    return (new_ss);
  }

  /** Returns the suppressee **/
  public NISuppressee get_suppressee() {
    return suppression_set[0].suppressee;
  }

  /**
   * Clears the suppressor state in each suppression.
   */
  public void clear_state () {
    for (int i = 0; i < suppression_set.length; i++ ) {
      suppression_set[i].clear_state();
    }
  }

  /**
   * Returns a string containing each suppression separated by commas.
   */
  public String toString() {
    return UtilMDE.join(suppression_set, ", ");
  }

}
