package daikon.suppress;

import daikon.*;
import daikon.inv.*;
import daikon.inv.binary.*;
import utilMDE.*;

import java.lang.reflect.*;
import java.util.logging.Logger;
import java.util.*;

/**
 * Class that defines a single non-instantiating suppression.  A suppression
 * consists of one or more suppressors and a suppressee.  If each of the
 * suppressors is true they imply the suppressee
 */
public class NISuppression {

  /** Set of suppressor invariants. **/
  NISuppressor[] suppressors;

  /** Suppressee invariant. **/
  NISuppressee suppressee;

  private boolean debug = false;
  static Stopwatch watch = new Stopwatch (false);

  public NISuppression (NISuppressor[] suppressor_set,
                        NISuppressee suppressee) {

    suppressors = suppressor_set;
    this.suppressee = suppressee;
  }

  public NISuppression (List<NISuppressor> suppressor_set,
                        NISuppressee suppressee) {

    suppressors =
      suppressor_set.toArray (new NISuppressor[suppressor_set.size()]);
    this.suppressee = suppressee;
  }

  public NISuppression (NISuppressor sup1, NISuppressee suppressee) {

    this(new NISuppressor[] {sup1}, suppressee);
  }

  public NISuppression (NISuppressor sup1, NISuppressor sup2,
                        NISuppressee suppressee) {

    this(new NISuppressor[] {sup1, sup2}, suppressee);
  }

  public NISuppression (NISuppressor sup1, NISuppressor sup2,
                        NISuppressor sup3, NISuppressee suppressee) {

    this(new NISuppressor[] {sup1, sup2, sup3}, suppressee);
  }

  public NISuppression (NISuppressor sup1, NISuppressor sup2,
                        NISuppressor sup3, NISuppressor sup4,
                        NISuppressee suppressee) {

    this(new NISuppressor[] {sup1, sup2, sup3, sup4}, suppressee);
  }

  public NISuppression (NISuppressor sup1, NISuppressor sup2,
                        NISuppressor sup3, NISuppressor sup4,
                        NISuppressor sup5, NISuppressee suppressee) {

    this(new NISuppressor[] {sup1, sup2, sup3, sup4, sup5}, suppressee);
  }

  public Iterator<NISuppressor> suppressor_iterator() {
    return Arrays.asList(suppressors).iterator();
  }

  /**
   * Checks this suppression.  Each suppressor is checked to see
   * if it matches inv and if not, whether or not it is valid (true).
   * The results are saved in each suppressor.  The suppressor results
   * are used later by @link{#invalidated()}
   *
   * @param ppt     Program point in which to check suppression
   * @param vis     Variables over which to check suppression
   * @param inv     Falsified invariant (if any).  Any suppressor
   *                that matches inv will be marked as NIS.MATCH
   *
   * @return NIS.VALID if the suppression is valid, NIS.MISSING if one or
   *         more suppressors were missing and the rest were valid,
   *         NIS.INVALID otherwise
   */
  public String check (PptTopLevel ppt, VarInfo[] vis, Invariant inv) {

    String status = NIS.VALID;
    boolean set = false;
    for (int i = 0; i < suppressors.length; i++) {
      NISuppressor ssor = suppressors[i];
      String st = ssor.check (ppt, vis, inv);

      if (!set) {
        if (st == NIS.MISSING)
          status = NIS.MISSING;
        else if (st != NIS.VALID) {
          status = (NIS.INVALID);
          if (st == NIS.INVALID) {
            return status;
          }
            // !valid in this case means invalid or match
            // If invalid, then stop immediately
            // otherwise, check state of the rest of the suppressors
            // This check is needed because we are reviving the
            // falsified method so match is now a valid status.
            set = true;
        }
      }
    }
    return (status);
  }

  /**
   * Determines whether or not the falsified invariant previously
   * passed to @link{#check(PptTopLevel,VarInfo[],Invariant)} was the
   * first suppressor to be falsified in this suppression.  If the
   * falsified invariant is not involved in this suppression, then it
   * can't have been invalidated.
   */
  public boolean invalidated() {

    // We return true when every suppressor except the falsified
    // one is valid and at least one suppressor matches the falsified
    // invariant.  Note that match can be true on more than one
    // suppressor due to reflexive (x, x, x) invariants.  In this
    // code, the suppressor should never be missing, since we should
    // have never looked at a slice with missing variables.
    boolean inv_match = false;
    for (int i = 0; i < suppressors.length; i++) {
      NISuppressor ssor = suppressors[i];
      Assert.assertTrue (ssor.state != NIS.MISSING);
      if (ssor.state == NIS.MATCH) {
        inv_match = true;
      } else if (ssor.state != NIS.VALID)
        return (false);
    }
    return (inv_match);
  }


  /**
   * Finds all of the invariants that are suppressed by this
   * suppression.
   *
   * @param suppressed_invs     Any invariants that are suppressed by
   *                            the antecedent invariants in ants
   *                            using this suppression are added to
   *                            this set.
   * @param ants                Antecedents organized by class
   */
  public void find_suppressed_invs (Set<NIS.SupInv> suppressed_invs,
                                    NIS.Antecedents ants) {

    // debug = suppressee.sup_class.getName().indexOf("NonZero") != -1;
    if (debug)
      Fmt.pf ("In find_suppressed_invs for " + this);

    // Get the antecedents that match our suppressors.  Return if there are
    // no antecedents for a particular suppressor.
    List<Invariant>[] antecedents = antecedents_for_suppressors (ants);
    if (antecedents == null)
      return;

    // Recursively check each combination of possible antecedents that
    // match our suppressors for suppressions
    VarInfo vis[] = new VarInfo[suppressee.var_count];
    find_suppressed_invs (suppressed_invs, antecedents, vis, 0);

    if (debug)
      Fmt.pf ("  suppressed invariants: " + suppressed_invs);

  }

  /**
   * Finds invariants that have become unsuppressed (one or more of
   * their antecedent invariants is falsified).  The invariant may
   * still be suppressed by a different suppression.
   *
   * @param unsuppressed_invs   Any invariants that are suppressed by
   *                            the antecedent invariants in ants
   *                            using this suppression are added to
   *                            this set if one or more of the antecedents
   *                            are falsified.
   * @param ants                Antecedents organized by class
   */
  public void find_unsuppressed_invs (Set<NIS.SupInv> unsuppressed_invs,
                                      NIS.Antecedents ants) {

    // debug = suppressee.sup_class.getName().indexOf("SeqIntLessEqual") != -1;

    // Get the antecedents that match our suppressors.  Return if there are
    // no antecedents for a particular suppressor.
    List<Invariant>[] antecedents = antecedents_for_suppressors (ants);
    if (antecedents == null)
      return;

    int total_false_cnt = 0;
    // Fmt.pf ("antecedents for suppression " + this);
    for (int i = 0; i < antecedents.length; i++) {
      List<Invariant> a = antecedents[i];
      int false_cnt = 0;
      for (Invariant inv : a) {
        if (inv.is_false())
          false_cnt++;
      }

      // Fmt.pf ("  suppressor %s: %s/%s", suppressors[i], "" + a.size(),
      //       "" + false_cnt);
      total_false_cnt += false_cnt;
    }

    if (total_false_cnt == 0)
      return;

    // Recursively check each combination of possible antecedents that
    // match our suppressors for suppressions
    VarInfo vis[] = new VarInfo[suppressee.var_count];
    // watch.clear();
    // watch.start();
    // int old_size = unsuppressed_invs.size();
    find_unsuppressed_invs (unsuppressed_invs, antecedents, vis, 0, false);
    // watch.stop();
    // Fmt.pf ("Found %s invariants in %s msecs",
    //        "" + (unsuppressed_invs.size() - old_size),
    //        "" + watch.elapsedMillis());
    if (debug)
      Fmt.pf ("  unsuppressed invariants: " + unsuppressed_invs);

  }

  /**
   * Recursively finds suppressed invariants.  The cross product
   * of antecedents for each suppressor are examined and each
   * valid combination will yield an entry in suppressed_invs.
   *
   * @param suppressed_invs     This set is updated with any invariants
   *                            that are suppressed,
   * @param antecedents         Array of antecedents per suppressor
   * @param vis                 Current variables for the suppressed invariant
   *                            As antecedents are chosen, their variables
   *                            are placed into vis.
   * @param idx                 Current index into suppressors and antecedents
   *
   * @see #find_unsuppressed_invs (Set, List, VarInfo[], int, boolean)
   * @see #consider_inv (Invariant, NISuppressor, VarInfo[])
   */
  private void find_suppressed_invs (Set<NIS.SupInv> unsuppressed_invs,
                                     List<Invariant>antecedents[],
                                     VarInfo vis[], int idx) {

    // Loop through each antecedent that matches the current suppressor
    NISuppressor s = suppressors[idx];
    for (Invariant inv : antecedents[idx]) {
      PptTopLevel ppt = inv.ppt.parent;

      // See if this antecedent can be used with the ones we have found so far
      VarInfo[] cvis = consider_inv (inv, s, vis);
      if (cvis == null)
        continue;

      // If this is the last suppressor
      if ((idx + 1) == suppressors.length) {

        // Create descriptions of the suppressed invariants
        List<NIS.SupInv> new_invs = suppressee.find_all (cvis, ppt);
        unsuppressed_invs.addAll (new_invs);

        // Check to insure that none of the invariants already exists
        if (Daikon.dkconfig_internal_check) {
          for (NIS.SupInv supinv : new_invs) {
            Invariant cinv = supinv.already_exists();
            if (cinv != null) {
              NISuppressionSet ss = cinv.get_ni_suppressions();
              ss.suppressed (cinv.ppt);
              Assert.assertTrue (false, "inv " + cinv.repr() + " of class "
                                 + supinv.suppressee + " already exists in ppt "
                                 + ppt.name + " suppressionset = " + ss
                                 + " suppression = " + this
                                 + " last antecedent = " + inv.format());
            }
          }
        }
      } else {
        // Recursively process the next suppressor
        find_suppressed_invs (unsuppressed_invs, antecedents, cvis, idx + 1);
      }
    }
  }

  /**
   * Recursively finds unsuppressed invariants.  The cross product
   * of antecedents for each suppressor is examined and each
   * valid combination with at least one falsified antecedent
   * will yield an entry in unsuppressed_invs.
   *
   * @param unsuppressed_invs   This set is updated with any invariants
   *                            that were suppressed, but one of the
   *                            suppressors is falsified (thus, the invariant
   *                            is no longer suppressed)
   * @param antecedents         Array of antecedents per suppressor
   * @param vis                 Current variables for the suppressed invariant
   *                            As antecedents are chosen, their variables
   *                            are placed into vis.
   * @param idx                 Current index into suppressors and antecedents
   * @param false_antecedents   True if a false antecedent has been found
   *
   * @see find_unsuppressed_invs (Set, List, VarInfo[], int)
   * @see #consider_inv (Invariant, NISuppressor, VarInfo[])
   */
  private void find_unsuppressed_invs (Set<NIS.SupInv> unsuppressed_invs,
                                       List<Invariant>antecedents[],
                                       VarInfo vis[], int idx,
                                       boolean false_antecedents) {

    boolean all_true_at_end = ((idx + 1) == suppressors.length)
      && !false_antecedents;

    // Loop through each antecedent that matches the current suppressor
    NISuppressor s = suppressors[idx];
    for (Invariant inv : antecedents[idx]) {
      PptTopLevel ppt = inv.ppt.parent;

      // If this is the last suppressor, no previous antecedents were
      // false, and this antecedent is not false either, we can stop
      // checking.  The antecedent lists are sorted so that the false
      // ones are first.  There is no need to look at antecedents that
      // are all true.
      if (all_true_at_end && !inv.is_false())
        return;

      // See if this antecedent can be used with the ones we have found so far
      VarInfo[] cvis = consider_inv (inv, s, vis);
      if (cvis == null)
        continue;

      // If this is the last suppressor
      if ((idx + 1) == suppressors.length) {

        // JHP: this check can be removed if the earlier check for all
        // true antecedents is included.
        if (!false_antecedents && !inv.is_false()) {
          if (debug)
            Fmt.pf ("Skipping %s, no false antecedents", VarInfo.toString(cvis));
          continue;
        }

        // Create descriptions of the suppressed invariants
        List<NIS.SupInv> new_invs = suppressee.find_all (cvis, ppt);
        if (debug)
          Fmt.pf ("created %s new invariants", new_invs);
        unsuppressed_invs.addAll (new_invs);

        // Check to insure that none of the invariants already exists
        if (Daikon.dkconfig_internal_check) {
          for (NIS.SupInv supinv : new_invs) {
            Invariant cinv = supinv.already_exists();
            if (cinv != null)
              Assert.assertTrue (false, "inv " + cinv.format() + " of class "
                                 + supinv.suppressee
                                 + " already exists in ppt " + ppt.name);

          }
        }
      } else {
        // Recursively process the next suppressor
        find_unsuppressed_invs (unsuppressed_invs, antecedents, cvis, idx + 1,
                                false_antecedents || inv.is_false());
      }
    }
  }

  /**
   * Determine if the specified invariant can be used as part of this
   * suppression.  The invariant must match suppressor and its variables
   * must match up with any antecedents that have been previously processed.
   * As invariants are processed by this method, their variables are added
   * to the slots in vis that correspond to their suppressor.
   *
   * For example, consider the invariant 'result = arg1 * arg2',
   * the suppression '(result=arg1) ^ (arg2=1)' and the invariants
   * 'x = y' and 'q = 1'.  If the varinfo_index of 'q' is less than
   * 'x' then it can't be used (because it would form an invalid
   * permutation.  Note that this set of antecedents will match
   * a different suppression for multiply that has a different
   * argument permutation.  More complex suppressions may refer
   * to the same variable more than once.  In those cases, the
   * antecedent invariants must also be over the same variables.
   *
   * @param inv         The invariant to attempt to add to the suppression.
   * @param supor       The suppressor we are trying to match.
   * @param vis         The current variables (if any) that have already
   *                    been determined by previous antecedents.
   *
   * @return a new VarInfo[] containing the variables of inv or null if inv
   * does not match in some way.
   */
  private VarInfo[] consider_inv (Invariant inv, NISuppressor supor,
                                  VarInfo[] vis) {

    // Make sure this invariant really matches this suppressor.  We know
    // the class already matches, but if the invariant has a swap variable
    // it must match as well
    if (!supor.match (inv))
      return (null);

    // Assign the variables from this invariant into vis.  If a variable
    // is already there and doesn't match this variable, then this
    // antecedent can't be used.
    VarInfo v1 = inv.ppt.var_infos[0];
    if ((vis[supor.v1_index] != null) && (vis[supor.v1_index] != v1))
      return (null);
    if ((supor.v2_index != -1) && (vis[supor.v2_index] != null)
        && (vis[supor.v2_index] != inv.ppt.var_infos[1]))
      return (null);
    VarInfo cvis[] = vis.clone();
    cvis[supor.v1_index] = v1;
    if (supor.v2_index != -1) {
      cvis[supor.v2_index] = inv.ppt.var_infos[1];
    }
    if (debug)
      Fmt.pf ("Placed antecedent '%s' into cvis %s", inv.format(),
              VarInfo.toString(cvis));

    // Make sure the resulting variables are in the proper order and are
    // compatible
    if (!vis_order_ok (cvis) || !vis_compatible (cvis)) {
      if (debug)
        Fmt.pf ("Skipping, cvis has bad order or is incompatible");
      return (null);
    }

    return (cvis);
  }

  /**
   * Builds an array of lists of antecedents that corresponds to each
   * suppressor in this suppression.  Returns null if the list is
   * empty for any suppressor (because that means there can't be
   * any suppressions based on these antecedents)
   */
  List<Invariant>[] antecedents_for_suppressors (NIS.Antecedents ants) {

    @SuppressWarnings("unchecked")
    List<Invariant> antecedents[] = (List<Invariant>[]) new List [suppressors.length];

    // Find the list of antecedents that matches each suppressor.  If any
    // suppressor doesn't have any matching antecedents, there can't be
    // any invariants that are suppressed by this suppression.
    for (int i = 0; i < suppressors.length; i++) {
      NISuppressor s = suppressors[i];
      List<Invariant> alist = ants.get (s.get_inv_class());
      if (alist == null)
        return (null);
      antecedents[i] = alist;
    }

    if (debug)
      Fmt.pf (suppressee.sup_class.getName() + " " +
              antecedents_for_suppression (antecedents));

    return (antecedents);
  }

  /**
   * Determines whether the order of the variables in vis a valid
   * permutations (i.e., their varinfo_index's are ordered).  Null
   * elements are ignored (and an all-null list is ok)
   */
  private boolean vis_order_ok (VarInfo[] vis) {

    VarInfo prev = vis[0];
    for (int i = 1; i < vis.length; i++) {
      if ((prev != null) && (vis[i] != null)) {
        if (vis[i].varinfo_index < prev.varinfo_index)
          return (false);
      }
      if (vis[i] != null)
        prev = vis[i];
    }
    return (true);
  }

  /**
   * Determines if the non-null entries in vis are comparable.  Returns
   * true if they are, false if they are not.
   * JHP: this should really be part of is_slice_ok
   */
  public static boolean vis_compatible (VarInfo[] vis) {

    // Unary vis are always compatble
    if (vis.length == 1)
      return (true);

    // Check binary
    if (vis.length == 2) {
      if ((vis[0] == null) || (vis[1] == null))
        return (true);

      if (vis[0].rep_type.isArray() == vis[1].rep_type.isArray())
        return (vis[0].compatible(vis[1]));
      else if (vis[0].rep_type.isArray())
        return (vis[0].eltsCompatible (vis[1]));
      else
        return (vis[1].eltsCompatible (vis[0]));
    }

    // Check ternary
    if ((vis[1] != null) && (vis[2] != null))
      if (!vis[1].compatible (vis[2]))
        return (false);

    if ((vis[0] != null) && (vis[2] != null))
      if (!vis[0].compatible (vis[2]))
        return (false);

    return (true);
  }

  public List<NISuppression> recurse_definition (NISuppressionSet ss) {

    NISuppressee sse = ss.get_suppressee();
    List<NISuppression> new_suppressions = new ArrayList<NISuppression>();

    // Create a list of all of our suppressors that don't match the suppressee
    // of ss
    List<NISuppressor> old_sors = new ArrayList<NISuppressor>();
    NISuppressor match = null;
    for (int i = 0; i < suppressors.length; i++) {
      if (suppressors[i].match (sse))
        match = suppressors[i];
      else
        old_sors.add (suppressors[i]);
    }

    // If we didn't match any suppressor there is nothing to do
    if (match == null)
      return (new_suppressions);

    // Right now this only works if we match exactly one suppressor
    Assert.assertTrue ((old_sors.size() + 1) == suppressors.length);

    // Create one new suppression for each suppression in ss.  The suppressee
    // of ss is replaced by one of the suppressions of ss.  Each suppressor
    // in ss have its variable indices modified to match the original
    // suppressor.
    for (int i = 0; i < ss.suppression_set.length; i++) {
      NISuppression s = ss.suppression_set[i];
      List<NISuppressor> sors = new ArrayList<NISuppressor> (old_sors);
      for (int j = 0; j < s.suppressors.length; j++)
        sors.add (s.suppressors[j].translate (match));
      new_suppressions.add(new NISuppression (sors, suppressee));
    }

    return (new_suppressions);
  }


  /**
   * Clears the suppressor state in each suppressor.
   */
  public void clear_state () {
    for (int i = 0; i < suppressors.length; i++) {
      suppressors[i].clear_state();
    }
  }

  /**
   * Returns 'suppressor && suppressor ... => suppressee'
   */
  public String toString() {
    return (UtilMDE.join(suppressors, " && ")
            + " ==> " + suppressee);
  }

  /**
   * Returns a string describing each of the antecedents for each suppressor
   */
  public String antecedents_for_suppression (List<Invariant>antecedents[]) {

    String sep = Global.lineSep;

    String out = "suppression " + this + sep;
    for (int i = 0; i < antecedents.length; i++) {
      out += "antecedents for suppressor " + i + sep;
      for (Invariant inv : antecedents[i]) {
        out += "    " + inv.format() + (inv.is_false() ? " [false]" : " t") + sep;
      }
    }
    return (out);
  }


}
