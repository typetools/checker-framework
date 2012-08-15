package daikon;

import daikon.inv.*;
import daikon.inv.unary.*;
import daikon.inv.binary.*;
import daikon.inv.ternary.*;
import daikon.suppress.*;
import daikon.inv.unary.scalar.*;
import daikon.inv.unary.string.*;
import daikon.inv.unary.sequence.*;
import daikon.inv.unary.stringsequence.*;
import daikon.inv.ternary.threeScalar.*;
import daikon.inv.binary.twoScalar.*;

import java.io.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import checkers.quals.*;

import utilMDE.*;

/**
 * Class that implements dynamic constants optimization.  This
 * optimization doesn't instantiate invariants over constant
 * variables (i.e., that that have only seen one value).  When the
 * variable receives a second value, invariants are instantiated and
 * are given the sample representing the previous constant value.
 **/
public class DynamicConstants implements Serializable {

  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20040401L;

  // If true don't create any invariants (including OneOfs) over dynamic
  // constants during post processing.  Normally, the configuration
  // variable OneOf_only is more appropriate
  static final boolean no_post_process = false;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.

  /**
   * Boolean. If true only create OneOf invariants for variables that
   * are constant for the entire run.  If false, all possible invariants
   * are created between constants.  Note that setting this to true only
   * fails to create invariants between constants.  Invariants between
   * constants and non-constants are created regardless.
   *
   * A problem occurs with merging when this is turned on.  If a var_info
   * is constant at one child slice, but not constant at the other child
   * slice, interesting invariants may not be merged because they won't
   * exist on the slice with the constant.  This is thus currently
   * defaulted to false.
   */
  public static boolean dkconfig_OneOf_only = false;

  /** Debug tracer. **/
  public static final Logger debug
                          = Logger.getLogger ("daikon.DynamicConstants");

  /** List of dynamic constants. **/
  List<Constant> con_list = new ArrayList<Constant>();

  /** List of variables that have always been missing. **/
  List<Constant> missing_list = new ArrayList<Constant>();

  /** List of all variables. **/
  Constant[] all_vars;
  List<Constant> all_list = new ArrayList<Constant>();

  /** Program point of these constants. **/
  PptTopLevel ppt = null;

  /** Number of sample received. **/
  int sample_cnt = 0;

  /**
   * Class used to store the value and count for each constant.
   * Note that two objects of this class are equal if they refer
   * to the same variable.  This allows these to be stored in
   * sets.
   **/
  public static class Constant implements Serializable {

    // We are Serializable, so we specify a version to allow changes to
    // method signatures without breaking serialization.  If you add or
    // remove fields, you should change this number to the current date.
    static final long serialVersionUID = 20030913L;

    /** The value of the constant. **/
    public /*@Interned*/ Object val;

    /** The sample count of the constant. **/
    public int count;

    /** The variable that has this value. **/
    public VarInfo vi;

    /** Whether or not this has been missing for every sample to date. **/
    boolean always_missing = true;

    /** Whether or not this is constant. **/
    boolean constant = false;

    /**
     * Whether or not this was constant at the beginning of this sample.
     * At the beginning of the add() method, all newly non constant variables
     * are marked (constant=false).  It is sometimes useful within the
     * remainder of processing that sample to know that a variable was
     * constant at the beginning.  The field previous_constant is set to
     * true when constant is set to false, and then is itself set to false
     * at the end of the add() method.
     */
    boolean previous_constant = false;

    /**
     * Whether or not this was always missing at the beginning of this sample.
     * At the beginning of the add() method, all newly non missing variables
     * are marked (always_missing=false).  It is sometimes useful within the
     * remainder of processing that sample to know that a variable was
     * missing at the beginning.  The field previous_missing  set to
     * true when missing is set to false, and then is itself set to false
     * at the end of the add() method.
     */
    boolean previous_missing = false;

    public Constant (VarInfo vi) {
      this.vi = vi;
      this.val = null;
      this.count = 0;
    }

    public boolean equals (Object obj) {
      if (!(obj instanceof Constant))
        return (false);
      Constant c = (Constant) obj;
      return (c.vi == vi);
    }

    public int hashCode() {
      return (vi.hashCode());
    }

    public String toString() {

      StringBuffer out = new StringBuffer();
      out.append (vi.name());
      if (val == null)
        out.append (" (val missing)");
      else
        out.append (" (val=" + val + ")");
      if (vi.isCanonical())
        out.append (" (leader) ");
      out.append (" [always_missing=" + always_missing + ", constant="
                  + constant + ", previous_constant=" + previous_constant
                  + ", previous_missing=" + previous_missing + "]");
      return (out.toString());
    }
  }

  /** Compares two constants based on the vi_index of their variable. **/
  public static final class ConIndexComparator
    implements Comparator<Constant>, Serializable {
    // We are Serializable, so we specify a version to allow changes to
    // method signatures without breaking serialization.  If you add or
    // remove fields, you should change this number to the current date.
    static final long serialVersionUID = 20050923L;

    private ConIndexComparator() {
    }

    public int compare(Constant con1, Constant con2) {
      return (con1.vi.varinfo_index - con2.vi.varinfo_index);
    }

    public static ConIndexComparator getInstance() {
      return theInstance;
    }
    static final ConIndexComparator theInstance = new ConIndexComparator();
  }

  /**
   * Create an initial list of constants and missing variables for the
   * specified ppt.
   */
  public DynamicConstants (PptTopLevel ppt) {

    this.ppt = ppt;

    // Start everything off as missing (since we haven't seen any values yet)
    all_vars = new Constant[ppt.var_infos.length];
    for (int i = 0; i < all_vars.length; i++) {
      VarInfo vi = ppt.var_infos[i];
      all_vars[i] = new Constant (vi);
      all_list.add (all_vars[i]);
      missing_list.add (all_vars[i]);
    }

  }

  /**
   * Checks each current constant to see if it is still a constant.
   * Constants must have the same value and cannot be missing.  In the
   * long run a better job of dealing with missing might be helpful.
   * Also checks each variable that has always been missing to date to
   * insure that it is still missing.
   *
   * Creates all new views required for the newly non constants (noncons)
   * and the newly non-missing (non_missing)
   */
  public void add (ValueTuple vt, int count) {

    List<Constant> non_missing = new ArrayList<Constant>();
    List<Constant> non_con = new ArrayList<Constant>();

    // Check each constant, destroy any that are missing or different
    for (Iterator<Constant> i = con_list.iterator(); i.hasNext(); ) {
      Constant con = i.next();
      /*@Interned*/ Object val = con.vi.getValue (vt);
      if (Debug.logDetail())
        Debug.log (getClass(), ppt, Debug.vis(con.vi), "Adding "
                   + Debug.toString(val) +
                   " to constant " + con.val +" : missing = "
                   + missing (con.vi, vt)
                  +": samples = " + con.count + "/" + count);
      if ((con.val != val) || missing (con.vi, vt)) {
        i.remove();
        con.constant = false;
        con.previous_constant = true;
        Assert.assertTrue (all_vars[con.vi.varinfo_index].constant == false);
        non_con.add (con);
      } else {
        con.count += count;
      }
    }

    // Move any non-missing variables to the constant list and init their val
    // If a variable is missing out of bounds, leave it on this list
    // forever (guranteeing that invariants will never be instantiated over
    // it).
    for (Iterator<Constant> i = missing_list.iterator(); i.hasNext(); ) {
      Constant con = i.next();
      if (con.vi.missingOutOfBounds())
        continue;
      /*@Interned*/ Object val = con.vi.getValue (vt);
      if (!missing (con.vi, vt)) {
        i.remove();
        con.always_missing = false;
        if (Debug.logDetail())
          Debug.log (getClass(), ppt, Debug.vis(con.vi), "Adding "
                     + Debug.toString(val) +
                     " to missing : missing = "
                     + missing (con.vi, vt)
                    + ": samples = " + con.count + "/" + count
                    + "/" + sample_cnt);
        if (sample_cnt == 0) {
          con.val = val;
          con.count = count;
          con.constant = true;
          con_list.add (con);
        } else {
          non_missing.add (con);
          con.previous_missing = true;
        }
      }
    }

    sample_cnt += count;

    // Create slices over newly non-constant and non-missing variables
    instantiate_new_views (non_con, non_missing);

    // Turn off previous_constant on all newly non-constants
    for (Constant con : non_con) {
      con.previous_constant = false;
    }

    // Turn off previous_missing on all newly non-missing
    for (Constant con : non_missing) {
      con.previous_missing = false;
    }
  }

  /** Returns whether the specified variable is missing in this ValueTuple. **/
  private boolean missing (VarInfo vi, ValueTuple vt) {

    int mod = vt.getModified (vi);
    return ((mod == ValueTuple.MISSING_FLOW)
            || (mod == ValueTuple.MISSING_NONSENSICAL));
  }

  /** Returns whether the specified variable is currently a constant. **/
  public boolean is_constant (VarInfo vi) {

    return (all_vars[vi.varinfo_index].constant);
  }

  /**
   * returns whether the specified variable is currently a constant OR
   * was a constant at the beginning of constants processing.
   **/
  public boolean is_prev_constant (VarInfo vi) {

    return (all_vars[vi.varinfo_index].constant
            || all_vars[vi.varinfo_index].previous_constant);
  }

  /**
   * Returns the constant value of the specified variable, or null if
   * the variable is not constant or prev_constant.
   **/
  public Object constant_value (VarInfo vi) {

    if (all_vars[vi.varinfo_index].constant
        || all_vars[vi.varinfo_index].previous_constant)
      return (all_vars[vi.varinfo_index].val);
    else
      return (null);
  }

  /** Returns whether the specified variable missing for all values so far. **/
  public boolean is_missing (VarInfo vi) {

    return (all_vars[vi.varinfo_index].always_missing);
  }

  /**
   * returns whether the specified variable is currently missing OR
   * was missing at the beginning of constants processing.
   **/
  public boolean is_prev_missing (VarInfo vi) {

    return (all_vars[vi.varinfo_index].always_missing
            || all_vars[vi.varinfo_index].previous_missing);
  }

  /** Returns the number of constants that are leaders. **/
  public int constant_leader_cnt() {

    int con_cnt = 0;
    for (Constant con : con_list) {
      if (con.vi.isCanonical())
        con_cnt++;
    }

    return (con_cnt);
  }

  /**
   * Creates all new views required for the newly non constants (noncons)
   * and the newly non-missing (non_missing).
   */
  public void instantiate_new_views (List<Constant> noncons,
                                     List<Constant> non_missing) {

    if (Debug.logOn()) {
      for (Constant con : noncons) {
        Debug.log (getClass(), ppt, Debug.vis(con.vi), "is non constant"
                    + " with val = " + Debug.toString(con.val)
                    + " with count = " + con.count);
      }
      for (Constant con : non_missing) {
        Debug.log (getClass(), ppt, Debug.vis(con.vi), "is non missing");
      }
    }


    // Create all of the views over noncons and noncons+con_list.
    // Since everything starts out as a constant, it is only necessary
    // to combine the newly non-constants with a combination of
    // the remaining constants and the newly-non constants.  Any slices
    // between the non-constants and other variables will have already
    // been created when those other variables became non-constants.
    if (noncons.size() > 0) {
      List<Constant> cons = new ArrayList<Constant>();
      cons.addAll (con_list);
      cons.addAll (noncons);
      debug.fine ("Instantiating non constants in ppt: " + ppt.name());
      instantiate_views (noncons, cons);
    }

    // Create all views over the newly non-missing.  Since missing
    // vars were not included in any previous views, we must match them
    // against all variables.
    if (non_missing.size() > 0) {
      debug.fine ("Instantiating non missing in ppt: " + ppt.name());
      instantiate_views (non_missing, all_list);
    }

    // Create any ternary invariants that are suppressed when one
    // of the variables is a constant.  Currently, only LinearTernary
    // falls into this list (It is suppressed by (x = C) && (Ay + Bz = D))
    if (NIS.dkconfig_enabled)
      instantiate_constant_suppressions (noncons, all_list);
  }

  /**
   * Instantiate views and invariants across each combination of
   * vars from list1 and list2.  If each item in a new slice
   * was a constant, the constant values are applied.
   *
   * The following slices will be created:
   *    unary:   list1-vars
   *    binary:  list1-vars X list2-vars
   *    ternary: list1-vars X list2-vars X list2-vars
   */
  private void instantiate_views (List<Constant> list1,
                                  List<Constant> list2) {

    // Get list1 leaders
    Set<Constant> leaders1 = new LinkedHashSet<Constant>();
    for (Constant con : list1) {
      if (con.vi.isCanonical())
        leaders1.add (con);
    }

    // Get list2 leaders
    Set<Constant> leaders2 = new LinkedHashSet<Constant>();
    for (Constant con : list2) {
      if (con.vi.isCanonical())
        leaders2.add (con);
    }

    if (debug.isLoggable (Level.FINE)) {
      debug.fine ("instantiating over " + leaders1.size()
                  + " leaders1: " + leaders1);
      debug.fine ("instantiating over " + leaders2.size()
                  + " leaders2: " + leaders2);
    }

    // any new views created
    Vector<PptSlice> new_views = new Vector<PptSlice>();

    int mod = ValueTuple.MODIFIED;

    // Unary slices/invariants
    for (Constant con: leaders1) {
      if (Debug.logOn())
        Debug.log (getClass(), ppt, Debug.vis(con.vi), "Considering slice");
      if (!ppt.is_slice_ok (con.vi))
         continue;
      PptSlice1 slice1 = new PptSlice1 (ppt, con.vi);
      slice1.instantiate_invariants();
      if (Debug.logOn())
        Debug.log (getClass(), ppt, Debug.vis(con.vi), "Instantiated invs");
      if (con.count > 0) {
        slice1.add_val_bu (con.val, mod, con.count);
      }
      new_views.add (slice1);
    }

    // Binary slices/invariants.
    for (Constant con1 : leaders1) {
      for (Constant con2 : leaders2) {
        Constant c1 = con1;
        Constant c2 = con2;
        Debug.log (getClass(), ppt, Debug.vis(c1.vi, c2.vi),
                   "Considering slice");
        if (con2.vi.varinfo_index < con1.vi.varinfo_index) {
          if (leaders1.contains (con2)) {
            // The variable is in both leader lists.
            // Don't add it on this iteration; add it when the variables
            // are given in order (to prevent creating the slice twice).
            continue;
          }
          c1 = con2;
          c2 = con1;
        }
        if (!ppt.is_slice_ok (c1.vi, c2.vi)) {
          if (Debug.logOn())
            Debug.log (debug, getClass(), ppt, Debug.vis(c1.vi, c2.vi),
                       "Not instantiating slice " + c1.vi.equalitySet.size());
          continue;
        }
        PptSlice2 slice2 = new PptSlice2 (ppt, c1.vi, c2.vi);
        slice2.instantiate_invariants();
        if (c1.count > 0 && c2.count > 0) {
          slice2.add_val_bu (c1.val, c2.val, mod, mod, con1.count);
        }
        new_views.add (slice2);
      }
    }

    // Ternary slices/invariants.  Note that if a variable is in both
    // leader lists, it is only added when it is in order (to prevent
    // creating the slice twice).
    for (Constant con1 : leaders1) {
      for (Constant con2 : leaders2) {
        if ((con2.vi.varinfo_index < con1.vi.varinfo_index)
            && leaders1.contains (con2))
          continue;
        for (Constant con3 : leaders2) {
          if ((con3.vi.varinfo_index < con2.vi.varinfo_index) ||
              ((con3.vi.varinfo_index < con1.vi.varinfo_index)
               && leaders1.contains (con3)))
            continue;
          Constant[] con_arr = {con1, con2, con3};
          Arrays.sort (con_arr, ConIndexComparator.getInstance());
          Assert.assertTrue ((con_arr[0].vi.varinfo_index
                              <= con_arr[1].vi.varinfo_index) &&
                             (con_arr[1].vi.varinfo_index
                              <= con_arr[2].vi.varinfo_index));
          if (!ppt.is_slice_ok (con_arr[0].vi, con_arr[1].vi, con_arr[2].vi))
            continue;

          PptSlice3 slice3 = new PptSlice3 (ppt, con_arr[0].vi, con_arr[1].vi,
                                            con_arr[2].vi);
          slice3.instantiate_invariants();
          if ((con_arr[0].count > 0) && (con_arr[1].count > 0)
              && (con_arr[2].count > 0)) {
            slice3.add_val_bu (con_arr[0].val, con_arr[1].val,
                              con_arr[2].val, mod, mod, mod, con_arr[0].count);
          }
          new_views.add (slice3);
        }
      }
    }

    // Debug print the created slies
    if (Debug.logOn() || debug.isLoggable (Level.FINE)) {
      int[] slice_cnt = {0, 0, 0, 0};
      int[] inv_cnt = {0, 0, 0, 0};
      int[] true_inv_cnt = {0, 0, 0, 0};
      for (PptSlice slice : new_views) {
        for (Invariant inv : slice.invs) {
          inv.log ("created, falsified = " + inv.is_false());
          if (!inv.is_false())
            true_inv_cnt[slice.arity()]++;
          else {
            String vals = "";
            for (VarInfo vi : slice.var_infos) {
              vals += vi.name() + "="
                + Debug.toString (constant_value(vi)) + " ";
            }
            inv.log ("Invariant " + inv.format()
                     + " destroyed by constant values" + vals);
          }
        }
        if (slice.invs.size() > 0)
          slice_cnt[slice.arity()]++;
        inv_cnt[slice.arity()] += slice.invs.size();
        if (Debug.logDetail()) {
          StringBuffer sb = new StringBuffer();
          for (int j = 0; j < slice.arity(); j++) {
            VarInfo v = slice.var_infos[j];
            sb.append (v.name() + " [" + v.file_rep_type +"] ["
                        + v.comparability + "] ");
          }
          Debug.log (debug, getClass(), ppt, slice.var_infos,
                      "Adding slice over " + sb + ": with " + slice.invs.size()
                      + " invariants" );
        }
      }
      for (int i = 1; i <= 3; i++)
        debug.fine ("Added " + slice_cnt[i] + " slice" + i + "s with "
                    + true_inv_cnt[i] + " invariants (" + inv_cnt[i]
                    + " total)");

      String leader1_str = "";
      int leader1_cnt = 0;
      for (Constant con1 : leaders1) {
        if (con1.vi.file_rep_type == ProglangType.INT) {
          leader1_str += con1.vi.name() + " ";
          leader1_cnt++;
        }
      }

      String leader2_str = "";
      int leader2_cnt = 0;
      for (Constant con1 : leaders2) {
        if (con1.vi.file_rep_type == ProglangType.INT) {
          leader2_str += con1.vi.name() + " ";
          leader2_cnt++;
        }
      }
      debug.fine (leader1_cnt + " leader1 ints (" + leader1_str + "): "
                  + leader2_cnt + " leader2 ints (" + leader2_str);
    }

    // Remove any falsified invariants from the new views.  Don't
    // call remove_falsified() since that has side-effects (such as
    // NIS processing on the falsified invariants) that we don't want.
    for (PptSlice slice : new_views) {
      List<Invariant> to_remove = new ArrayList<Invariant>();
      for (Invariant inv : slice.invs) {
        if (inv.is_false()) {
          to_remove.add(inv);
        }
      }
      slice.removeInvariants (to_remove);
    }

    // Add the new slices to the top level ppt.  This will discard any
    // slices that ended up with zero invariants
    ppt.addViews (new_views);

  }

  public void instantiate_constant_suppressions (List<Constant> new_noncons,
                                                 List<Constant> all) {

    // Find all of the variable (non-constant) non-missing
    // integral/float leaders
    List<Constant> vars = new ArrayList<Constant>();
    for (Constant con : all) {
      if (con.always_missing || con.previous_missing)
        continue;
      if (con.constant || con.previous_constant)
        continue;
      if (!con.vi.isCanonical())
        continue;
      if (!con.vi.file_rep_type.isIntegral() &&!con.vi.file_rep_type.isFloat())
        continue;
      if (con.vi.rep_type.isArray())
        continue;
      vars.add (con);
    }

    // Find all of the new non-constant integer/float leaders
    List<Constant> new_leaders = new ArrayList<Constant>();
    for (Constant con : new_noncons) {
      if (!con.vi.isCanonical())
        continue;
      if (!con.vi.file_rep_type.isIntegral() &&!con.vi.file_rep_type.isFloat())
        continue;
      if (con.vi.rep_type.isArray())
        continue;
      new_leaders.add (con);
    }

    if (debug.isLoggable (Level.FINE)) {
      debug.fine ("new non-con leaders = " + new_leaders);
      debug.fine ("variable leaders = " + vars);
    }

    // Consider all of the ternary slices with one new non-constant
    for (int i = 0; i < new_leaders.size(); i++) {
      Constant con1 = new_leaders.get(i);
      for (int j = 0; j < vars.size(); j++ ) {
        Constant con2 = vars.get(j);
        Assert.assertTrue (con1 != con2);
        for (int k = j; k < vars.size(); k++ ) {
          Constant con3 = vars.get(k);
          Assert.assertTrue (con1 != con3);
          if (!ppt.is_slice_ok (con1.vi, con2.vi, con3.vi))
            continue;

          if (debug.isLoggable (Level.FINE))
            debug.fine (Fmt.spf ("considering slice %s %s %s", con1, con2,
                                 con3));

          // Look for a linearbinary over two variables.  If it doesn't
          // exist we don't create a LinearTernary
          Invariant lb = find_linear_binary (ppt.findSlice (con2.vi,con3.vi));
          if (lb == null)
            continue;

          // Find the ternary slice and create it if it is not there
          PptSlice slice = ppt.get_or_instantiate_slice (con1.vi, con2.vi,
                                                         con3.vi);

          // Create the LinearTernary invariant from the LinearBinary
          // invariant and the constant value
          Invariant lt = null;
          if (con1.vi.file_rep_type.isIntegral()) {
            lt = LinearTernary.get_proto().instantiate (slice);
            if (lt != null)
              ((LinearTernary) lt).setup ((LinearBinary) lb, con1.vi,
                        ((Long) con1.val).longValue());
          } else /* must be float */ {
            lt = LinearTernaryFloat.get_proto().instantiate (slice);
            if (lt != null)
              ((LinearTernaryFloat) lt).setup ((LinearBinaryFloat) lb, con1.vi,
                        ((Double) con1.val).doubleValue());
          }
          if (lt != null) {
            if (Daikon.dkconfig_internal_check)
              Assert.assertTrue
                (slice.find_inv_by_class (lt.getClass()) == null,
                "inv = " + lt.format() + " slice = " + slice);
            slice.addInvariant (lt);
            debug.fine ("Adding invariant " + lt.format() + " to slice "
                        + slice);
          }
        }
      }
    }

    // Consider all of the ternary slices with two new non-constants
    for (int i = 0; i < new_leaders.size(); i++) {
      Constant con1 = new_leaders.get(i);
      for (int j = i; j < new_leaders.size(); j++ ) {
        Constant con2 = new_leaders.get(j);
        for (int k = 0; k < vars.size(); k++ ) {
          Constant con3 = vars.get(k);
          Assert.assertTrue (con2 != con3);
          Assert.assertTrue (con1 != con3);
          if (!ppt.is_slice_ok (con1.vi, con2.vi, con3.vi))
            continue;

          if (debug.isLoggable (Level.FINE))
            debug.fine (Fmt.spf ("considering slice %s %s %s", con1, con2,
                                 con3));

          // Create the ternary slice

          // Create the LinearTernary invariant from the OneOf invariant
          // (if any) and the constant values.  If no OneOf exists,
          // there can be no interesting plane of the points
          Invariant lt = null;
          PptSlice slice = null;
          InvariantStatus sts = InvariantStatus.NO_CHANGE;
          if (con1.vi.file_rep_type.isIntegral()) {
            OneOfScalar oo = (OneOfScalar) ppt.find_inv_by_class
                (new VarInfo[] {con3.vi}, OneOfScalar.class);
            if (oo == null)
              continue;
            slice = ppt.get_or_instantiate_slice (con1.vi, con2.vi, con3.vi);

            lt = LinearTernary.get_proto().instantiate (slice);
            if (lt != null)
              sts = ((LinearTernary) lt).setup (oo, con1.vi,
                        ((Long) con1.val).longValue(),
                        con2.vi, ((Long) con2.val).longValue());
          } else /* must be float */ {
            OneOfFloat oo = (OneOfFloat) ppt.find_inv_by_class
                (new VarInfo[] {con3.vi}, OneOfFloat.class);
            if (oo == null)
              continue;
            slice = ppt.get_or_instantiate_slice (con1.vi, con2.vi, con3.vi);
            lt = LinearTernaryFloat.get_proto().instantiate (slice);
            if (lt != null)
              sts = ((LinearTernaryFloat) lt).setup (oo, con1.vi,
                        ((Double) con1.val).doubleValue(),
                        con2.vi, ((Double) con2.val).doubleValue());
          }
          if ((lt != null) && (sts == InvariantStatus.NO_CHANGE)) {
            if (Daikon.dkconfig_internal_check)
              Assert.assertTrue
                (slice.find_inv_by_class (lt.getClass()) == null,
                "inv = " + lt.format() + " slice = " + slice);
            slice.addInvariant (lt);
            debug.fine ("Adding invariant " + lt.format() + " to slice "
                        + slice);
          }
        }
      }
    }

  }

  /**
   * Looks for a LinearBinary invariant in the specified slice.
   * Will match either float or integer versions
   */
  private Invariant find_linear_binary (PptSlice slice) {

    // if (debug.isLoggable (Level.FINE))
    //  debug.fine ("considering slice " + slice);

    if (slice == null)
      return (null);

    for (Invariant inv : slice.invs) {
      // debug.fine ("inv = " + inv.getClass());
      if ((inv.getClass() == LinearBinary.class)
          || (inv.getClass() == LinearBinaryFloat.class))
        return (inv);
    }

    return (null);
  }

  /**
   * Create invariants for any remaining constants.  Right now, this looks
   * for invariants between all of the constants.  Its not clear that
   * between constants are interesting, but to match previous behavior, this
   * is what we will do for now.
   */
  public void post_process () {

    // if requested, don't create any post-processed invariants
    if (no_post_process) {
      int con_count = 0;
      for (Constant con : con_list) {
        if (!con.vi.isCanonical())
          continue;
        System.out.println ("  Not creating invariants over leader "
                            + con.vi.name() + " = " + con.val);
        con_count++;
      }
      System.out.println (con_count + " constants at ppt " + ppt);
      return;
    }

    // If specified, create only OneOf invariants.  Also create a reflexive
    // equality invariant, since that is assumed to exist in many places
    if (dkconfig_OneOf_only) {
      for (Constant con : con_list) {
        if (!con.vi.isCanonical())
          continue;
        instantiate_oneof (con);
        ppt.create_equality_inv (con.vi, con.vi, con.count);
      }
      return;
    }

    // Get a list of all remaining constants and clear the existing list
    // (if the existing list is not cleared, constant slices will not
    // be created)
    List<Constant> noncons = con_list;
    for (Constant con : con_list) {
      con.constant = false;
      con.previous_constant = true;
    }
    con_list = new ArrayList<Constant>();

    // Don't do anything with variables that have always been missing.  They
    // should have no invariants over them.
    List<Constant> non_missing = new ArrayList<Constant>();

    instantiate_new_views (noncons, non_missing);

  /* Code to just create just unary slices for constants
    for (Constant con : con_list) {
      if (!con.vi.isCanonical())
        continue;
      PptSlice1 slice1 = new PptSlice1 (ppt, con.vi);
      slice1.instantiate_invariants();
      if (con.val != null)
        slice1.add_val (con.val, ValueTuple.MODIFIED, con.count);
      new_views.add (slice1);
    }
    ppt.addViews (new_views);
  */
  }

  /**
   * Create unary and binary constant invariants.  The slices and
   * invariants are created and returned, but not added to the
   * ppt.  Note that when NIS.dkconfig_suppressor_list is turned
   * on (default is on), only unary and binary invariants that can
   * be suppressors in NIS suppressions are created.
   */
  public List<PptSlice> create_constant_invs() {

    // Turn off track logging so that we don't get voluminous messages
    // each time this is called
    boolean debug_on = Logger.getLogger("daikon.Debug").isLoggable(Level.FINE);
    if (debug_on)
      LogHelper.setLevel ("daikon.Debug", Level.OFF);

    // Get constant leaders
    List<Constant> leaders = new ArrayList<Constant>(100);
    for (Constant con : con_list) {
      if (!con.vi.isCanonical())
        continue;

      // hashcode types are not involved in suppressions
      if (NIS.dkconfig_skip_hashcode_type) {
        if (con.vi.file_rep_type.isHashcode()) {
          continue;
        }
      }

      leaders.add (con);
    }

    List<PptSlice> new_views = new ArrayList<PptSlice>(100);
    int mod = ValueTuple.MODIFIED;

    // Unary slices/invariants
    for (Constant con : leaders) {

      PptSlice1 slice1 = new PptSlice1(ppt, con.vi);

      if (NIS.dkconfig_suppressor_list) {
        slice1.instantiate_invariants(NIS.suppressor_proto_invs);
      } else {
        slice1.instantiate_invariants();
      }

      // Fmt.pf ("%s = %s, [%s] count = %s  ", con.vi.name(), con.val,

      if (con.count > 0) {
        slice1.add_val_bu(con.val, mod, con.count);
      }
      if (slice1.invs.size() > 0)
        new_views.add(slice1);
    }


    // Binary slices/invariants
    for (int i = 0; i < leaders.size(); i++) {
      Constant con1 = leaders.get(i);
      for (int j = i; j < leaders.size(); j++) {
        Constant con2 = leaders.get(j);
        if (!con1.vi.compatible(con2.vi))
          continue;

        PptSlice2 slice2 = new PptSlice2(ppt, con1.vi, con2.vi);
        if (NIS.dkconfig_suppressor_list) {
          slice2.instantiate_invariants(NIS.suppressor_proto_invs);
        } else {
          slice2.instantiate_invariants();
        }

        if (con1.count > 0 && con2.count > 0) {
          slice2.add_val_bu(con1.val, con2.val, mod, mod, con1.count);
        }
        if (slice2.invs.size() > 0)
          new_views.add(slice2);
      }
    }

    // Remove any falsified invariants from the new views.
    for (PptSlice slice : new_views) {
      for (Iterator<Invariant> j = slice.invs.iterator(); j.hasNext(); ) {
        Invariant inv = j.next();
        if (inv.is_false()) {
          j.remove();
        }
      }
    }

    if (debug_on)
      LogHelper.setLevel ("daikon.Debug", Level.FINE);

    return (new_views);
  }

  public void print_missing (PrintWriter out) {

    for (Constant con : missing_list) {
      out.println (con.vi.name() + " is always missing");
    }
  }

  /**
   * Merge dynamic constants from the children of this ppt.  Only missing
   * is merged since constants are not used after we are done processing
   * samples.
   */
  public void merge () {

    // clear the constant and missing lists
    missing_list.clear();
    con_list.clear();

    // Process each variable at this ppt.  If the variable is missing at
    // each of the children, it is also missing here.  Ignore children that
    // have no mapping for this variable
    for (VarInfo pvar : ppt.var_infos) {
      boolean missing = true;
      for (PptRelation rel : ppt.children) {
        VarInfo cvar = rel.childVar (pvar);
        if ((cvar != null) && (rel.child.constants != null)
            && !rel.child.constants.is_missing (cvar)) {
          missing = false;
          break;
        }
      }
      all_vars[pvar.varinfo_index].always_missing = missing;
      if (missing)
        missing_list.add (all_vars[pvar.varinfo_index]);

    }
  }

  /**
   * Creates OneOf invariants for each constant
   */
  public void instantiate_oneof (Constant con) {

    Invariant inv = null;
    PptSlice1 slice1 = (PptSlice1) ppt.get_or_instantiate_slice (con.vi);

    // Create the correct OneOf invariant
    ProglangType rep_type = con.vi.rep_type;
    boolean is_scalar = rep_type.isScalar();
    if (is_scalar) {
      inv = OneOfScalar.get_proto().instantiate (slice1);
    } else if (rep_type == ProglangType.INT_ARRAY) {
      inv = OneOfSequence.get_proto().instantiate (slice1);
    } else if (Daikon.dkconfig_enable_floats
               && rep_type == ProglangType.DOUBLE) {
      inv = OneOfFloat.get_proto().instantiate (slice1);
    } else if (Daikon.dkconfig_enable_floats
               && rep_type == ProglangType.DOUBLE_ARRAY) {
      inv = OneOfFloatSequence.get_proto().instantiate (slice1);
    } else if (rep_type == ProglangType.STRING) {
      inv = OneOfString.get_proto().instantiate (slice1);
    } else if (rep_type == ProglangType.STRING_ARRAY) {
      inv = OneOfStringSequence.get_proto().instantiate (slice1);
    } else {
      // Do nothing; do not even complain
    }
    slice1.addInvariant (inv);

    // Add the value to it
    slice1.add_val_bu (con.val, ValueTuple.MODIFIED, con.count);
  }
}
