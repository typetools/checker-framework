package daikon;

import static daikon.Global.lineSep;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import utilMDE.Assert;
import utilMDE.UtilMDE;
import daikon.Daikon.TerminationMessage;
import daikon.inv.Invariant;
import daikon.inv.InvariantStatus;
import daikon.inv.ValueSet;
import daikon.suppress.NIS;

/**
 * DaikonSimple reads a declaration file and trace file and outputs a list of
 * likely invariants using the simple incremental algorithm. Its methods
 * parallel those of Daikon but oftentimes certain checks are eliminated from
 * DaikonSimple's methods because there is less filtering of invariants and
 * variables.
 *
 * DaikonSimple was written to check the implementation of the optimizations in
 * Daikon. DaikonSimple does not use an optimizations, and its processing will
 * produce a complete set of true invariants. Daikon does have flags to "turn
 * off" some of its optimizations but there are some optimizations are built
 * into the way Daikon processes the samples (e.g. variable hierarchy and bottom
 * up processing). In addition, we want to check the optimizations, so we don't
 * want to bypass them. In Daikon, code was written to "undo" the optimizations,
 * so we could recover the invariants that were previously filtered out or not
 * created (see Daikon.dkconfig_undo_opts flag). By comparing the output from
 * the two, we can find problems with the optimization implementation by
 * tracking the cause of the differences.
 */
public class DaikonSimple {

  // logging information
  public static final Logger debug = Logger.getLogger("daikon.DaikonSimple");

  public static final Logger debug_detail = Logger
      .getLogger("daikon.DaikonSimple.Detail");

  // inv file for storing the invariants in serialized form
  public static File inv_file = null;

  private static String usage = UtilMDE
      .join(
          new String[] {
              "",
              "Usage: java daikon.DaikonSimple [OPTION]... <decls_file> <dtrace_file>",
              "  -h, --" + Daikon.help_SWITCH,
              "      Display this usage message",
              "  -o, <inv_file> ",
              "      Writes output to <inv_file>",
              "  --" + Daikon.debugAll_SWITCH,
              "      Turns on all debug flags (voluminous output)",
              "  --" + Daikon.debug_SWITCH + " logger",
              "      Turns on the specified debug logger",
              "  --" + Daikon.track_SWITCH + " class<var1,var2,var3>@ppt",
              "      Print debug info on the specified invariant class, vars, and ppt", },
          lineSep);

  // a pptMap that contains all the program points
  public static PptMap all_ppts;

  public static void main(final String[] args) throws IOException,
      FileNotFoundException {

    try {
      mainHelper(args);
    } catch (Daikon.TerminationMessage e) {
      String message = e.getMessage();
      if (Daikon.dkconfig_show_stack_trace)
        e.printStackTrace();
      if (message != null) {
        System.err.println(message);
        System.exit(1);
      }
      System.exit(0);
    }
    // Any exception other than Daikon.TerminationMessage gets propagated.
    // This simplifies debugging by showing the stack trace.
  }

  /**
   * This does the work of main, but it never calls System.exit, so it is
   * appropriate to be called progrmmatically. Termination of the program with a
   * message to the user is indicated by throwing Daikon.TerminationMessage.
   *
   * Difference from Daikon's mainHelper: turn off optimization flags (equality,
   * dynamic constants, NIS suppression).
   *
   * @see #main(String[])
   * @see daikon.Daikon.TerminationMessage
   * @see daikon.Daikon#mainHelper(String[])
   */
  public static void mainHelper(final String[] args) throws IOException,
      FileNotFoundException {

    // set up logging information
    daikon.LogHelper.setupLogs(daikon.LogHelper.INFO);

    // No optimizations used in the simple incremental algorithm so
    // optimizations are turned off.
    Daikon.use_equality_optimization = false;
    Daikon.dkconfig_use_dynamic_constant_optimization = false;
    Daikon.suppress_implied_controlled_invariants = false;
    NIS.dkconfig_enabled = false;

    // The flag tells FileIO and Daikon to use DaikonSimple
    // specific methods (e.g. FileIO.read_declaration_file).
    // When FileIO reads and processes
    // samples, it must use the SimpleProcessor rather than the
    // default Processor.
    Daikon.using_DaikonSimple = true;

    // Read command line options
    Daikon.FileOptions files = Daikon.read_options(args, usage);
    // DaikonSimple does not supply nor use the spinfo_files and map_files
    Set<File> decls_files = files.decls;
    Set<String> dtrace_files = files.dtrace;

    if ((decls_files.size() == 0) && (dtrace_files.size() == 0)) {
      throw new Daikon.TerminationMessage(
          "No .decls or .dtrace files specified");
    }

    // Create the list of all invariant types
    Daikon.setup_proto_invs();

    // Create the program points for enter and numbered exits and
    // initializes the points (adding orig and derived variables)
    all_ppts = FileIO.read_declaration_files(decls_files);

    // Create the combined exits (and add orig and derived vars)
    // Daikon.create_combined_exits(all_ppts);

    // Read and process the data trace files
    SimpleProcessor processor = new SimpleProcessor();
    FileIO.read_data_trace_files(dtrace_files, all_ppts, processor, true);

    //System.exit(0);

    // Print out the invariants for each program point (sort first)
    for (Iterator<PptTopLevel> t = all_ppts.pptIterator(); t.hasNext();) {
      PptTopLevel ppt = t.next();

      // We do not need to print out program points that have not seen
      // any samples.
      if (ppt.num_samples() == 0) {
        continue;
      }
      List<Invariant> invs = PrintInvariants.sort_invariant_list(ppt
          .invariants_vector());
      List<Invariant> filtered_invs = Daikon.filter_invs(invs);
      // The dkconfig_quiet printing is used for creating diffs between
      // DaikonSimple
      // and Daikon's output. The second kind of printing is used for
      // debugging. Since the names of the program points are the same for both
      // Daikon and DaikonSimple, diffing the two output will result in
      // only differences in the invariants, but we can not see at which program
      // points these differing invariants appear. Using the second kind of
      // printing,
      // Daikon's output does not have the '+' in the program point name, so in
      // addition
      // to the invariants showing up in the diff, we will also see the program
      // point
      // names.

      if (Daikon.dkconfig_quiet) {
        System.out
            .println("====================================================");
        System.out.println(ppt.name());
      } else {
        System.out
            .println("===================================================+");
        System.out.println(ppt.name() + " +");
      }

      // Sometimes the program points actually differ in number of
      // samples seen due to differences in how Daikon and DaikonSimple
      // see the variable hierarchy.
      System.out.println(ppt.num_samples());

      for (Invariant inv : filtered_invs) {
        System.out.println(inv.getClass());
        System.out.println(inv);
      }
    }
  }

  /**
   * Install views and the invariants. Duplicated from PptTopLevel's version
   * because DaikonSimple needs to use its own version of slice checking code.
   *
   * Difference from PptTopLevel's version: 1. canonical (leader of equality
   * set) check of variables is turned off because every variable is in its own
   * equality set 2. debugging information turned off because DaikonSimple's
   * code is more contained 3. less constraints on the slices
   *
   * @see daikon.PptTopLevel#instantiate_views_and_invariants()
   */

  // Note that some slightly inefficient code has been added to aid
  // in debugging. When creating binary and ternary views and debugging
  // is on, the outer loops will not terminate prematurely on innapropriate
  // (i.e., non-canonical) variables. This allows explicit debug statements
  // for each possible combination, simplifying determining why certain
  // slices were not created.
  //
  // Note that '///*' indicates code duplicated from PptTopLevel's
  // version but commented out because DaikonSimple does not need
  // to perform these checks
  public static void instantiate_views_and_invariants(PptTopLevel ppt) {

    // used only for debugging
    int old_num_vars = ppt.var_infos.length;
    int old_num_views = ppt.numViews();
    boolean debug_on = debug.isLoggable(Level.FINE);

    // / 1. all unary views

    // Unary slices/invariants.
    // Currently, there are no constraints on the unary
    // slices. Since we are trying to create all of the invariants, the
    // variables does not have to be a leader and can be a constant.
    // Note that the always missing check is only applicable when the
    // dynamic constants optimization is turned on (so we do not do the
    // check here).

    Vector<PptSlice> unary_views = new Vector<PptSlice>(ppt.var_infos.length);
    for (VarInfo vi : ppt.var_infos) {

      // /* if (!is_slice_ok(vi))
      // /* continue;

      PptSlice1 slice1 = new PptSlice1(ppt, vi);
      slice1.instantiate_invariants();

      unary_views.add(slice1);
    }
    ppt.addViews(unary_views);
    unary_views = null;

    // / 2. all binary views

    // Binary slices/invariants.
    Vector<PptSlice> binary_views = new Vector<PptSlice>();
    for (int i1 = 0; i1 < ppt.var_infos.length; i1++) {
      VarInfo var1 = ppt.var_infos[i1];

      // Variables can be constant and missing in DaikonSimple invariants
      // /* if (!is_var_ok_binary(var1))
      // /* continue;

      for (int i2 = i1; i2 < ppt.var_infos.length; i2++) {
        VarInfo var2 = ppt.var_infos[i2];

        // Variables can be constant and missing in DaikonSimple invariants
        // /* if (!is_var_ok_binary(var2))
        // /* continue;

        if (! (var1.compatible(var2)
            || (var1.type.isArray() && var1.eltsCompatible(var2))
            || (var2.type.isArray() && var2.eltsCompatible(var1)))) {
          continue;
        }

        PptSlice2 slice2 = new PptSlice2(ppt, var1, var2);
        slice2.instantiate_invariants();

        binary_views.add(slice2);
      }
    }
    ppt.addViews(binary_views);
    binary_views = null;

    // 3. all ternary views
    Vector<PptSlice> ternary_views = new Vector<PptSlice>();
    for (int i1 = 0; i1 < ppt.var_infos.length; i1++) {
      VarInfo var1 = ppt.var_infos[i1];

      if (!is_var_ok(var1))
        continue;

      for (int i2 = i1; i2 < ppt.var_infos.length; i2++) {
        VarInfo var2 = ppt.var_infos[i2];

        if (!is_var_ok(var2))
          continue;

        for (int i3 = i2; i3 < ppt.var_infos.length; i3++) {
          VarInfo var3 = ppt.var_infos[i3];

          if (!is_var_ok(var3))
            continue;

          if (!is_slice_ok(var1, var2, var3)) {
            continue;
          }
          PptSlice3 slice3 = new PptSlice3(ppt, var1, var2, var3);
          slice3.instantiate_invariants();
          ternary_views.add(slice3);
        }
      }
    }

    ppt.addViews(ternary_views);

    // This method didn't add any new variables.
    Assert.assertTrue(old_num_vars == ppt.var_infos.length);
    ppt.repCheck();

  }

  // This method is exclusively for checking variables participating
  // in ternary invariants. The variable must be integer or float, and
  // can not be an array.
  public static boolean is_var_ok(VarInfo var) {

    return (var.file_rep_type.isIntegral() || var.file_rep_type.isFloat())
        && !var.rep_type.isArray();

  }

  /**
   * Returns whether or not the specified binary slice should be created. The
   * slice should not be created if the vars not compatible.
   *
   * Since we are trying to create all of the invariants, the variables does not
   * have to be a leader and can be a constant. Note that the always missing
   * check is only applicable when the dynamic constants optimization is turned
   * on (so we do not do the check here).
   *
   * @see daikon.PptTopLevel#is_slice_ok(VarInfo, VarInfo)
   *
   */
  public static boolean is_slice_ok(VarInfo v1, VarInfo v2) {

    return v1.compatible(v2);
  }

  /**
   * Returns whether or not the specified ternary slice should be created. The
   * slice should not be created if any of the following are true - Any var is
   * an array - Any of the vars are not compatible with the others - Any var is
   * not (integral or float)
   *
   * Since we are trying to create all of the invariants, the variables does not
   * have to be a leader and can be a constant. Note that the always missing
   * check is only applicable when the dynamic constants optimization is turned
   * on (so we do not do the check here). In addition, we do want to create the
   * reflexive ones and partially reflexive invariants.
   *
   * @see daikon.PptTopLevel#is_slice_ok(VarInfo, VarInfo, VarInfo)
   *
   */
  public static boolean is_slice_ok(VarInfo v1, VarInfo v2, VarInfo v3) {

    // Vars must be compatible
    return (v1.compatible(v2) && v1.compatible(v3) && v2.compatible(v3));
  }

  /**
   * The Call class helps the SimpleProcessor keep track of matching enter and
   * exit program points and also object program points. Each Call object
   * represents one entry in the dtrace file, i.e. enter, exit, object entry.
   *
   */
  static final class Call {

    public PptTopLevel ppt;

    public ValueTuple vt;

    public Call(PptTopLevel ppt, ValueTuple vt) {

      this.ppt = ppt;
      this.vt = vt;
    }
  }

  /**
   * The SimpleProcessor class processes each sample in the dtrace file.
   *
   */
  public static class SimpleProcessor extends FileIO.Processor {
    PptMap all_ppts = null;

    /** nonce -> List<Call,Call> * */
    // The first Call is the enter entry and the second is the object entry
    Map<Integer, List<Call>> call_map = new LinkedHashMap<Integer, List<Call>>();

    // Flag for whether there are out of order entries in the
    // dtrace file. For unterminated calls (enter but
    // not exit entry in the dtrace file), because DaikonSimple had
    // processed each entry separately (not bottom up like Daikon),
    // DaikonSimple applied the enter and object call before seeing the
    // exit call, which is not consistent with Daikon. Daikon does not
    // process unterminated method calls.

    // The method of holding the enter and object calls until finding
    // a matching exit call assumes:
    // - enter always comes before exit
    // - first entry in dtrace is an enter
    // - order in dtrace is enter, exit, object [for constructors] or
    // enter, object, exit, object [for methods] but not necessarily
    // sequential
    boolean wait = false;

    // pointer to last nonce so we can associate the object entry
    // with the right enter entry
    Integer last_nonce = new Integer(-1);


    /**
     * Creates a valuetuple for the receiver using the vt of the original.  The
     * method copies over the values of variables shared by both program points
     * and sets the rest of the variables in the receiver's valuetuple as missing.
     * Also, adds the orig and derived variables to the receiver and returns the
     * newly created valuetuple.
     */
    private static ValueTuple copySample(PptTopLevel receiver,
        PptTopLevel original, ValueTuple vt, int nonce) {

      // Make the vt for the receiver ppt
//      Object values[] = new Object[receiver.num_tracevars];
//      int mods[] = new int[receiver.num_tracevars];
      Object values[] = new Object[receiver.var_infos.length - receiver.num_static_constant_vars];
      int mods[] = new int[receiver.var_infos.length - receiver.num_static_constant_vars];

      // Build the vt for the receiver ppt by looking through the current
      // vt and filling in the gaps.
      int k = 0;
      for (Iterator<VarInfo> i = receiver.var_info_iterator(); i.hasNext();) {

        VarInfo var = i.next();
        if (var.is_static_constant)
          continue;
        boolean found = false;
        for (Iterator<VarInfo> j = original.var_info_iterator(); j.hasNext();) {
          VarInfo var2 = j.next();

          if (var.name().equals(var2.name())) {
            values[k] = vt.getValue(var2);
            mods[k] = vt.getModified(var2);
            found = true;
            break;
          }
        }

        if (!found) {
          values[k] = null;
          mods[k] = 2;
        }
        k++;

      }

      ValueTuple receiver_vt = new ValueTuple(values, mods);

      FileIO.add_orig_variables(receiver, receiver_vt.vals, receiver_vt.mods,
          nonce);
      FileIO
          .add_derived_variables(receiver, receiver_vt.vals, receiver_vt.mods);

      return receiver_vt;

    }

    /**
     * process the sample by checking it against each existing invariant at the
     * program point and removing the invariant from the list of possibles if
     * any invariant is falsified.
     */
    public void process_sample(PptMap all_ppts, PptTopLevel ppt, ValueTuple vt,
        Integer nonce) {
      this.all_ppts = all_ppts;

      // Add samples to orig and derived variables
      FileIO.add_orig_variables(ppt, vt.vals, vt.mods, nonce);
      FileIO.add_derived_variables(ppt, vt.vals, vt.mods);

      // Intern the sample
      vt = new ValueTuple(vt.vals, vt.mods);

      // DaikonSimple must make the object program point manually because
      // the new Chicory produced dtrace files do not contain object ppts
      // in the dtrace part of the file (the program point is declared).

      // Make the object ppt
      PptName ppt_name = ppt.ppt_name;

      PptTopLevel object_ppt = null;
      PptTopLevel class_ppt = null;
      ValueTuple object_vt = null;
      ValueTuple class_vt = null;

      if ((ppt_name.isEnterPoint() && !ppt_name.isConstructor())
          || ppt_name.isExitPoint()) {
        object_ppt = all_ppts.get(ppt_name.makeObject());
        class_ppt = all_ppts.get(ppt_name.makeClassStatic());
      }

      // C programs do not have object ppts
      // check whether the ppt is a static or instance method
      // that decides whether the sample is copied over to the object and/or
      // class ppt
      if (object_ppt != null) {

          // the check assumes that static fields are not stored first in the
          // object ppt
          if (ppt.find_var_by_name (object_ppt.var_infos[0].name()) != null) {
            // object and class ppt should be created
            object_vt = copySample(object_ppt, ppt, vt, nonce);

            if (class_ppt != null) {
                class_vt = copySample(class_ppt, ppt, vt, nonce);
            }

          } else {
            // only class ppt should be created
            if (class_ppt != null) {
              class_vt = copySample(class_ppt, ppt, vt, nonce);
            }

            object_vt = null;
            object_ppt = null;
          }
        }

      // If this is an enter point, just remember it for later
      if (ppt_name.isEnterPoint()) {
        Assert.assertTrue(nonce != null);
        Assert.assertTrue(call_map.get(nonce) == null);
        List<Call> value = new ArrayList<Call>();
        value.add(new Call(ppt, vt));

        if (object_ppt != null) {
          value.add(new Call(object_ppt, object_vt));
        }

        if (class_ppt != null) {
          value.add(new Call(class_ppt, class_vt));
        }

        call_map.put(nonce, value);
        last_nonce = nonce;
        wait = true;
        return;
      }

      // If this is an exit point, process the saved enter (and sometimes
      // object) point
      if (ppt_name.isExitPoint()) {
        Assert.assertTrue(nonce != null);
        List<Call> value = call_map.remove(nonce);

        add(ppt, vt, nonce);

        for (Call ec : value) {
          add(ec.ppt, ec.vt, nonce);
        }
        wait = false;
      }

      if (object_ppt != null)
        add(object_ppt, object_vt, nonce); // apply object vt

      if (class_ppt != null)
        add(class_ppt, class_vt, nonce);
    }

    // The method iterates through all of the invariants in the ppt
    // and manually adds the sample to the invariant and removing the
    // invariant if it is falsified

    private void add(PptTopLevel ppt, ValueTuple vt, int nonce) {

      // if this is a numbered exit, apply to the combined exit as well
      if (ppt.ppt_name.isNumberedExitPoint()) {

        // Daikon.create_combined_exits(all_ppts);
        PptTopLevel parent = all_ppts.get(ppt.ppt_name.makeExit());
        if (parent != null) {
          parent.get_missingOutOfBounds(ppt, vt);
          add(parent, vt, nonce);

        } else {
          // make parent and apply

          // this is a hack. it should probably filter out orig and derived
          // vars instead of taking the first n.
          int len = ppt.num_tracevars + ppt.num_static_constant_vars;
          VarInfo[] exit_vars = new VarInfo[len];
          for (int j = 0; j < len; j++) {
            exit_vars[j] = new VarInfo(ppt.var_infos[j]);
            exit_vars[j].varinfo_index = ppt.var_infos[j].varinfo_index;
            exit_vars[j].value_index = ppt.var_infos[j].value_index;
            exit_vars[j].equalitySet = null;
          }

          parent = new PptTopLevel(ppt.ppt_name.makeExit().getName(), exit_vars);
          Daikon.init_ppt(parent, all_ppts);
          all_ppts.add(parent);
          parent.get_missingOutOfBounds(ppt, vt);
          add(parent, vt, nonce);
        }
      }


      // If the point has no variables, skip it
      if (ppt.var_infos.length == 0) {
        // The sample should be skipped but Daikon does not do this so
        // DaikonSimple will not do this to be consistent.
        // The better idea is for Daikon to assert that these valuetuples are
        // empty and then skip the sample.
        assert vt.size() == 0;
        return;
      }

      // Instantiate slices and invariants if this is the first sample
      if (ppt.num_samples() == 0) {
        instantiate_views_and_invariants(ppt);
      }

      // manually inc the sample number because DaikonSimple does not
      // use any of PptTopLevel's add methods which increase the sample
      // number
      ppt.incSampleNumber();

      // Loop through each slice
      for (Iterator<PptSlice> i = ppt.views_iterator(); i.hasNext();) {
        PptSlice slice = i.next();
        Iterator<Invariant> k = slice.invs.iterator();
        boolean missing = false;

        for (VarInfo v : slice.var_infos) {
          // If any var has encountered out of array bounds values,
          // stop all invariants in this slice. The presumption here is that
          // an index out of bounds implies that the derived variable (eg a[i])
          // doesn't really make any sense (essentially that i is not a valid
          // index for a). Invariants on the derived variable are thus not
          // relevant.
          // If any variables are out of bounds, remove the invariants
          if (v.missingOutOfBounds()) {
            while (k.hasNext()) {
              Invariant inv = k.next();
              k.remove();
            }
            missing = true;
            break;
          }

          // If any variables are missing, skip this slice
          if (v.isMissing(vt)) {
            missing = true;
            break;
          }
        }

        // keep a list of the falsified invariants
        if (!missing) {
          while (k.hasNext()) {

            Invariant inv = k.next();
            Invariant pre_inv = inv.clone();
            InvariantStatus status = inv.add_sample(vt, 1);
            if (status == InvariantStatus.FALSIFIED) {
              k.remove();
            }
          }
        }

        // update num_samples and num_values of a slice manually
        // because DaikonSimple does not call any of PptTopLevel's
        // add methods
        for (int j = 0; j < vt.vals.length; j++) {
          if (!vt.isMissing(j)) {
              ValueSet vs = ppt.value_sets[j];
              vs.add(vt.vals[j]);
          }
        }
        ppt.mbtracker.add(vt, 1);

      }
    }
  }
}
