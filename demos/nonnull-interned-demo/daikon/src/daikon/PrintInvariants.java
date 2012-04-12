package daikon;

import java.util.*;
import java.io.*;
import gnu.getopt.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.*;

import utilMDE.*;
import daikon.derive.*;
import daikon.derive.binary.*;
import daikon.inv.*;
import daikon.inv.OutputFormat;
import daikon.inv.filter.*;
import daikon.suppress.*;
import daikon.config.Configuration;

public final class PrintInvariants {

  private PrintInvariants() { throw new Error("do not instantiate"); }

  /**
   * See dkconfig_replace_prestate.
   *
   * Resets the prestate expression mapping.
   * This is done between printing of each different
   * program point.
   */
  public static void resetPrestateExpressions() {
    varNameCounter = 0;
    exprToVar = new HashMap<String,String>();
  }

  // Used to create distinct variable names (see See dkconfig_replace_prestate).
  private static int varNameCounter = 0;

  // Maps prestate expressions to variable names (see See dkconfig_replace_prestate)>
  private static Map<String,String> exprToVar = new HashMap<String,String>();

  /**
   * See dkconfig_replace_prestate.
   *
   * Return the variable name corresponding to expr.  Create a new
   * varname and an expr -> varname mapping if there is not already
   * one.
   */
  public static String addPrestateExpression(String expr) {
    if (expr == null) {
      throw new IllegalArgumentException(expr);
    }
    if (exprToVar.containsKey(expr)) {
      return exprToVar.get(expr);
    }
    String v = "v" + Integer.toString(varNameCounter++);
    exprToVar.put(expr, v);
    return v;
  }

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.

  /**
   * This option must be given with "--format Java" option.
   *
   * Instead of outputting prestate expressions as "\old(E)" within an
   * invariant, output a variable names (e.g. `v1'). At the end of
   * each program point, output the list of variable-to-expression
   * mappings. For example: with this option set to false, a program
   * point might print like this:
   *
   * foo.bar.Bar(int):::EXIT
   * \old(capacity) == sizeof(this.theArray)
   *
   * With the option set to true, it would print like this:
   *
   * foo.bar.Bar(int):::EXIT
   * v0 == sizeof(this.theArray)
   * prestate assignment: v0=capacity
   *
   */
  public static boolean dkconfig_replace_prestate = true;

  /**
   * Print invariant classname with invariants in output of
   * <code>format()</code> method, normally used only for debugging output
   * rather than ordinary printing of invariants.
   **/
  public static boolean dkconfig_print_inv_class = false;

  /** If true, print all invariants without any filtering.  **/
  public static boolean dkconfig_print_all = false;

  /**
   * If true, print the total number of true invariants.  This includes
   * invariants that are redundant and would normally not be printed
   * or even created due to optimizations
   */
  public static boolean dkconfig_true_inv_cnt = false;

  /**
   * If true, remove as many variables as possible that need to be indicated
   * as 'post'.  Post variables occur when the subscript for a derived
   * variable with an orig sequence is not orig.  For example: orig(a[post(i)])
   * An equivalent expression involving only orig variables is substitued
   * for the post variable when one exists.
   */
  public static boolean dkconfig_remove_post_vars = false;

  /**
   * In the new decl format print array names without as 'a[]' as
   * opposed to 'a[..]'  This creates names that are more compatible
   * with the old output.  This option has no effect in the old decl
   * format
   */
  public static boolean dkconfig_old_array_names = true;

  /**
   * This enables a different way of treating static constant variables.
   * They are not created into invariants into slices.  Instead, they are
   * examined during print time.  If a unary invariant contains a value
   * which matches the value of a static constant varible, the value
   * will be replaced by the name of the variable, "if it makes sense".
   * For example, if there is a static constant variable a = 1.  And there
   * exists an invariant x <= 1, x <= a would be the result printed.
   */
  public static boolean dkconfig_static_const_infer = false;

  /**
   * Main debug tracer for PrintInvariants (for things unrelated to printing).
   **/
  public static final Logger debug = Logger.getLogger("daikon.PrintInvariants");

  /** Debug tracer for printing. **/
  public static final Logger debugRepr
    = Logger.getLogger("daikon.PrintInvariants.repr");

  /** Debug tracer for printing. **/
  public static final Logger debugPrint = Logger.getLogger("daikon.print");

  /** Debug tracer for printing modified variables in ESC/JML/DBC output. **/
  public static final Logger debugPrintModified
    = Logger.getLogger("daikon.print.modified");

  /** Debug tracer for printing equality. **/
  public static final Logger debugPrintEquality
    = Logger.getLogger("daikon.print.equality");

  /** Debug tracer for filtering. **/
  public static final Logger debugFiltering
    = Logger.getLogger("daikon.filtering");

  /** Debug tracer for variable bound information. **/
  public static final Logger debugBound  = Logger.getLogger ("daikon.bound");

  private static final String lineSep = Global.lineSep;

  /** Whether we are doing output for testing.  Used only for IOA output. **/
  public static boolean test_output = false;

  /** Regular expression that ppts must match to be printed **/
  private static Pattern ppt_regexp;

  /**
   * Switch for whether to print discarded Invariants or not, default is false.
   * Activated by --disc_reason switch.
   **/
  public static boolean print_discarded_invariants = false;

  /**
   * If true, then each invariant is printed using the current
   * OutputFormat, but it's wrapped inside xml tags, along with other
   * information about the invariant.  For example, if this switch is
   * true and if the output format is JAVA, and the invariant prints
   * as "x == null", the results of print_invariant would look
   * something like:
   *
   * <INVINFO>
   * <INV> x == null </INV>
   * <SAMPLES> 100 </SAMPLES>
   * <DAIKON> x == null </DAIKON>
   * <DAIKONCLASS> daikon.inv.unary.scalar.NonZero </DAIKONCLASS>
   * <METHOD> foo() </METHOD>
   * </INVINFO>
   *
   * The above output is actually all in one line, although in this
   * comment it's broken up into multiple lines for clarity.
   *
   * Note the extra information printed with the invariant: the number
   * of samples from which the invariant was derived, the daikon
   * representation (i.e. the Daikon output format), the Java class
   * that the invariant corresponds to, and the method that the
   * invariant belongs to ("null" for object invariants).
   */
  public static boolean wrap_xml = false;

  // Fields that will be used if the --disc_reason switch is used
  private static String discClass = null;
  private static String discVars = null;
  private static String discPpt = null;

  // Avoid problems if daikon.Runtime is loaded at analysis (rather than
  // test-run) time.  This might have to change when JTrace is used.
  static { daikon.Runtime.no_dtrace = true; }

  private static String usage =
    UtilMDE.joinLines(
      "Usage: java daikon.PrintInvariants [OPTION]... FILE",
      "  -h, --" + Daikon.help_SWITCH,
      "      Display this usage message",
      "  --" + Daikon.format_SWITCH + " format_name",
      "      Write output in the given format.",
      "  --" + Daikon.suppress_redundant_SWITCH,
      "      Suppress display of logically redundant invariants.",
      "  --" + Daikon.output_num_samples_SWITCH,
      "      Output number of values and samples for invariants and ppts; for debugging.",
      "  --" + Daikon.config_option_SWITCH + " config_var=val",
      "      Sets the specified configuration variable.  ",
      "  --" + Daikon.debugAll_SWITCH,
      "      Turns on all debug flags (voluminous output)",
      "  --" + Daikon.debug_SWITCH + " logger",
      "      Turns on the specified debug logger",
      "  --" + Daikon.track_SWITCH + " class<var1,var2,var3>@ppt",
      "      Print debug info on the specified invariant class, vars, and ppt"
      );

  public static void main(final String[] args)
    throws FileNotFoundException, StreamCorruptedException,
           OptionalDataException, IOException, ClassNotFoundException {
    try {
      mainHelper(args);
    } catch (Configuration.ConfigException e) {
      System.err.println(e.getMessage());
    } catch (Daikon.TerminationMessage e) {
      System.err.println(e.getMessage());
      System.exit(1);
    }
    // Any exception other than Daikon.TerminationMessage gets propagated.
    // This simplifies debugging by showing the stack trace.
  }

  /**
   * This does the work of main, but it never calls System.exit, so it
   * is appropriate to be called progrmmatically.
   * Termination of the program with a message to the user is indicated by
   * throwing Daikon.TerminationMessage.
   * @see #main(String[])
   * @see daikon.Daikon.TerminationMessage
   **/
  public static void mainHelper(String[] args)
    throws FileNotFoundException, StreamCorruptedException,
           OptionalDataException, IOException, ClassNotFoundException {
    daikon.LogHelper.setupLogs(daikon.LogHelper.INFO);

    LongOpt[] longopts = new LongOpt[] {
      new LongOpt(Daikon.help_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.format_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.suppress_redundant_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.output_num_samples_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.config_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.config_option_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.debugAll_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.debug_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.ppt_regexp_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.track_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
    };
    Getopt g = new Getopt("daikon.PrintInvariants", args, "h", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch(c) {
      case 0:
        // got a long option
        String option_name = longopts[g.getLongind()].getName();
        if (Daikon.help_SWITCH.equals(option_name)) {
          System.out.println(usage);
          throw new Daikon.TerminationMessage();
        } else if (Daikon.ppt_regexp_SWITCH.equals (option_name)) {
          if (ppt_regexp != null)
            throw new Error("multiple --" + Daikon.ppt_regexp_SWITCH
                  + " regular expressions supplied on command line");
          try {
            String regexp_string = g.getOptarg();
            ppt_regexp = Pattern.compile(regexp_string);
          } catch (Exception e) {
            throw new Error(e);
          }
        } else if (Daikon.disc_reason_SWITCH.equals(option_name)) {
          try { PrintInvariants.discReasonSetup(g.getOptarg()); }
          catch (IllegalArgumentException e) {
            throw new Daikon.TerminationMessage(e.getMessage());
          }
        } else if (Daikon.suppress_redundant_SWITCH.equals(option_name)) {
          Daikon.suppress_redundant_invariants_with_simplify = true;
        } else if (Daikon.format_SWITCH.equals(option_name)) {
          String format_name = g.getOptarg();
          Daikon.output_format = OutputFormat.get(format_name);
          if (Daikon.output_format == null) {
            throw new Daikon.TerminationMessage(
              "Unknown output format:  --format " + format_name);
          }
        } else if (Daikon.output_num_samples_SWITCH.equals(option_name)) {
          Daikon.output_num_samples = true;
        } else if (Daikon.config_SWITCH.equals(option_name)) {
          String config_file = g.getOptarg();
          try {
            InputStream stream = new FileInputStream(config_file);
            Configuration.getInstance().apply(stream);
          } catch (IOException e) {
            throw new RuntimeException("Could not open config file "
                                        + config_file);
          }
          break;
        } else if (Daikon.config_option_SWITCH.equals(option_name)) {
          String item = g.getOptarg();
          daikon.config.Configuration.getInstance().apply(item);
          break;
        } else if (Daikon.debugAll_SWITCH.equals(option_name)) {
          Global.debugAll = true;
        } else if (Daikon.debug_SWITCH.equals(option_name)) {
          LogHelper.setLevel(g.getOptarg(), LogHelper.FINE);
        } else if (Daikon.track_SWITCH.equals (option_name)) {
          LogHelper.setLevel("daikon.Debug", LogHelper.FINE);
          String error = Debug.add_track (g.getOptarg());
          if (error != null) {
            throw new Daikon.TerminationMessage ("Error parsing track argument '"
                                + g.getOptarg() + "' - " + error);
          }
        } else {
          throw new RuntimeException("Unknown long option received: " +
                                     option_name);
        }
        break;
      case 'h':
        System.out.println(usage);
        throw new Daikon.TerminationMessage();
      case '?':
        break; // getopt() already printed an error
      default:
        System.out.println("getopt() returned " + c);
        break;
      }
    }

    validateGuardNulls();

    // The index of the first non-option argument -- the name of the file
    int fileIndex = g.getOptind();
    if (args.length - fileIndex != 1) {
        System.out.println(usage);
        throw new Daikon.TerminationMessage("Wrong number of arguments (expected 1)");
    }

    // Read in the invariants
    String filename = args[fileIndex];
    PptMap ppts = FileIO.read_serialized_pptmap(new File(filename),
                                                true // use saved config
                                                );
    // Setup the list of proto invariants and initialize NIS suppressions
    Daikon.setup_proto_invs();
    Daikon.setup_NISuppression();

    // Make sure ppts' rep invariants hold
    ppts.repCheck();

    // If requested, just print the number of true invariants
    if (dkconfig_true_inv_cnt) {
      print_true_inv_cnt (ppts);
      return;
    }

    validateGuardNulls();
//     if ((Daikon.dkconfig_guardNulls == "always") // interned
//         || (Daikon.dkconfig_guardNulls == "missing")) { // interned
//       Daikon.guardInvariants(ppts);
//     }

    // Debug print the hierarchy is a more readable manner
    if (debug.isLoggable(Level.FINE)) {
      debug.fine ("Printing PPT Hierarchy");
      for (Iterator<PptTopLevel> i = ppts.pptIterator(); i.hasNext(); ) {
        PptTopLevel my_ppt = i.next();
        if (my_ppt.parents.size() == 0)
          my_ppt.debug_print_tree (debug, 0, null);
      }
    }

    print_invariants(ppts);
  }

  // To avoid the leading "UtilMDE." on all calls.
  private static String nplural(int n, String noun) {
    return UtilMDE.nplural(n, noun);
  }

  /**
   * Prints out all the discardCodes and discardStrings of the Invariants
   * that will not be printed if the --disc_reason switch is used.
   **/
  public static void print_reasons(PptMap ppts) {
    if (!print_discarded_invariants || Daikon.no_text_output) {
      return;
    }

    System.out.println();
    System.out.println("DISCARDED INVARIANTS:");
    // DiscReasonMap.debug(discPpt);

    // Makes things faster if a ppt is specified
    if (discPpt != null) {
      PptTopLevel ppt = ppts.get(discPpt);
      if (ppt==null) {
        System.out.println("No such ppt found: "+discPpt);
      }
      else {
        String toPrint = "";
        toPrint += print_reasons_from_ppt(ppt,ppts);

        StringTokenizer st = new StringTokenizer(toPrint, lineSep);
        if (st.countTokens() > 2)
          System.out.print(toPrint);
        else {
          String matching = "";
          if (discVars!=null || discClass!=null)
            matching = " matching ";
          System.out.println("No" + matching + "discarded Invariants found in "
                             +ppt.name());
        }
      }
      return;
    }

    // Uses the custom comparator to get the Ppt objects in sorted order
    Comparator<PptTopLevel> comparator = new Ppt.NameComparator();
    TreeSet<PptTopLevel> ppts_sorted = new TreeSet<PptTopLevel>(comparator);
    ppts_sorted.addAll(ppts.asCollection());

    // Iterate over the PptTopLevels in ppts
    for (PptTopLevel ppt : ppts_sorted) {
      StringBuffer toPrint = new StringBuffer();
      toPrint.append(print_reasons_from_ppt(ppt,ppts));

      // A little hack so that PptTopLevels without discarded Invariants of
      // interest don't get their names printed
      StringTokenizer st = new StringTokenizer(toPrint.toString(), lineSep);
      if (st.countTokens() > 2) {
        System.out.print(toPrint.toString());
      }
    }
  }

  /** Validate guardNulls config option. **/
  public static void validateGuardNulls() {
    Daikon.dkconfig_guardNulls = Daikon.dkconfig_guardNulls.intern();
    // Complicated default!
    if (Daikon.dkconfig_guardNulls == "default") { // interned
      if (Daikon.output_format == OutputFormat.JML
          || Daikon.output_format == OutputFormat.ESCJAVA) {
        Daikon.dkconfig_guardNulls = "missing";
      } else {
        Daikon.dkconfig_guardNulls = "never";
      }
    }
    if (! ((Daikon.dkconfig_guardNulls == "always") // interned
           || (Daikon.dkconfig_guardNulls == "never") // interned
           || (Daikon.dkconfig_guardNulls == "missing")) // interned
        ) {
      throw new Error("Bad guardNulls config option \"" + Daikon.dkconfig_guardNulls + "\", should be one of \"always\", \"never\", or \"missing\"");
    }
  }


  /**
   * Add discard reasons for invariants that are filtered out
   */
  private static void add_filter_reasons(PptTopLevel ppt, PptMap ppts) {
    InvariantFilters fi = InvariantFilters.defaultFilters();
    for (Iterator<Invariant> fullInvItor = ppt.invariants_iterator();
         fullInvItor.hasNext(); ) {
      Invariant nextInv = fullInvItor.next();
      InvariantFilter varFilter = fi.shouldKeepVarFilters(nextInv);
      if (varFilter != null) {
        DiscReasonMap.put(nextInv, DiscardCode.findCode(varFilter),
                          varFilter.getDescription());
      } else {
        InvariantFilter propFilter = fi.shouldKeepPropFilters(nextInv);
        if (propFilter != null) {
          DiscardInfo di;
          if (propFilter instanceof ObviousFilter) {
            di = nextInv.isObvious();
            if (nextInv.logOn())
              nextInv.log ("DiscardInfo's stuff: " + di.className() + lineSep
                           + di.format());
          } else if (propFilter instanceof UnjustifiedFilter) {
            di = new DiscardInfo(nextInv, DiscardCode.bad_confidence,
                                 "Had confidence: " + nextInv.getConfidence());
          } else {
            di = new DiscardInfo(nextInv, DiscardCode.findCode(propFilter),
                                 propFilter.getDescription());
          }
          DiscReasonMap.put(nextInv, di);
        }
      }
    }
  }


  private static String print_reasons_from_ppt(PptTopLevel ppt, PptMap ppts) {
    // Add all the reasons that would come from filtering to the DiscReasonMap
    add_filter_reasons(ppt, ppts);

    String toPrint = "";
    String dashes = "--------------------------------------------"
                  + "-------------------------------" + lineSep;

    if (!(ppt instanceof PptConditional)) {
      toPrint += "==============================================="
              + "============================" + lineSep;
      toPrint += (ppt.name() + lineSep);
    }

    StringBuffer sb = new StringBuffer();
    for (DiscardInfo nextInfo : DiscReasonMap.returnMatches_from_ppt
           (new InvariantInfo(ppt.name(), discVars, discClass))) {
      sb.append(dashes + nextInfo.format() + lineSep);
    }

    // In case the user is interested in conditional ppt's
    if (Daikon.dkconfig_output_conditionals
          && Daikon.output_format == OutputFormat.DAIKON) {
      for (Iterator<PptConditional> i = ppt.cond_iterator(); i.hasNext() ; ) {
        PptConditional pcond = i.next();
        sb.append(print_reasons_from_ppt(pcond,ppts));
      }
    }
    return (toPrint + sb.toString());
  }

  /**
   * Method used to setup fields if the --disc_reason switch is used
   * if (arg==null) then show all discarded Invariants, otherwise just
   * show the ones specified in arg, where arg =
   * <class-name><<var1>,<var2>,...>@<ppt.name> e.g.:
   * OneOf<x>@foo():::ENTER would only show OneOf Invariants that
   * involve x at the program point foo:::ENTER (any of the 3 params
   * can be ommitted, e.g. OneOf@foo:::ENTER)
   * @throws IllegalArgumentException if arg is not of the proper syntax
   */
  public static void discReasonSetup(String arg) {
    print_discarded_invariants = true;
    usage = "Usage: <class-name><<var1>,<var2>,,,,>@<ppt.name()>" + lineSep +
            "or use --disc_reason \"all\" to show all discarded Invariants" + lineSep +
            "e.g.: OneOf<x>@foo():::ENTER" + lineSep;

    // Will print all discarded Invariants in this case
    if (arg==null || arg.length()==0 || arg.equals("all"))
      return;

    // User wishes to specify a classname for the discarded Invariants of
    // interest
    char firstChar = arg.charAt(0);
    // This temp is used later as a way of "falling through" the cases
    String temp = arg;
    if (firstChar!='@' && firstChar!='<') {
      StringTokenizer splitArg = new StringTokenizer(arg,"@<");
      discClass = splitArg.nextToken();
      if ((arg.indexOf('<') != -1) && (arg.indexOf('@') != -1) && (arg.indexOf('@') < (arg.indexOf('<'))))
        temp = arg.substring(arg.indexOf('@')); // in case the pptname has a < in it
      else if (arg.indexOf('<') != -1)
        temp = arg.substring(arg.indexOf('<'));
      else if (arg.indexOf('@') != -1)
        temp = arg.substring(arg.indexOf('@'));
      else
        return;
    }
    firstChar = temp.charAt(0);

    // User wants to specify the variable names of interest
    if (firstChar=='<') {
      if (temp.length() < 2)
        throw new IllegalArgumentException("Missing '>'" + lineSep +usage);
      if (temp.indexOf('>',1) == -1)
        throw new IllegalArgumentException("Missing '>'" + lineSep +usage);
      StringTokenizer parenTokens = new StringTokenizer(temp,"<>");
      if ((temp.indexOf('@')==-1 && parenTokens.countTokens() > 0)
          || (temp.indexOf('@')>-1 && parenTokens.countTokens() > 2))
        throw new IllegalArgumentException("Too many brackets" + lineSep +usage);
      StringTokenizer vars = new StringTokenizer(parenTokens.nextToken(),",");
      if (vars.hasMoreTokens()) {
        discVars = vars.nextToken();
        while (vars.hasMoreTokens())
          discVars += "," + vars.nextToken();
        // Get rid of *all* spaces since we know varnames can't have them
        discVars = discVars.replaceAll(" ", "");
      }
      if (temp.endsWith(">"))
        return;
      else {
        if (temp.charAt(temp.indexOf('>')+1) != '@')
          throw new IllegalArgumentException("Must have '@' after '>'" + lineSep +usage);
        else
          temp = temp.substring(temp.indexOf('>')+1);
      }
    }

    // If it made it this far, the first char of temp has to be '@'
    Assert.assertTrue(temp.charAt(0) == '@');
    if (temp.length()==1)
      throw new IllegalArgumentException("Must provide ppt name after '@'" + lineSep +usage);
    discPpt = temp.substring(1);
  }

  // The following code is a little odd because it is trying to match the
  // output format of V2.  In V2, combined exit points are printed after
  // the original exit points (rather than before as they are following
  // the PptMap sort order).
  //
  // Also, V2 only prints out a single ppt when there is only one
  // exit point.  This seems correct.  Probably a better solution to
  // this would be to not create the combined exit point at all when there
  // is only a single exit.  Its done here instead so as not to futz with
  // the partial order stuff.
  //
  // All of this can (and should be) improved when V2 is dropped.

  public static void print_invariants(PptMap all_ppts) {

    PrintWriter pw = new PrintWriter(System.out, true);
    PptTopLevel combined_exit = null;
    boolean enable_exit_swap = true; // !Daikon.dkconfig_df_bottom_up;

    if (Daikon.no_text_output)
      return;

    // Retrieve Ppt objects in sorted order.  Put them in an array list
    // so that it is easier to look behind and ahead.
    PptTopLevel[] ppts = new PptTopLevel [all_ppts.size()];
    int ii = 0;
    for (Iterator<PptTopLevel> itor = all_ppts.pptIterator() ; itor.hasNext() ; )
      ppts[ii++] = itor.next();

    for (int i = 0 ; i < ppts.length; i++) {
      PptTopLevel ppt = ppts[i];

      if (debug.isLoggable(Level.FINE))
        debug.fine ("Looking at point " + ppt.name());

      // If this point is not an exit point, print out any retained combined
      // exit point
      if (enable_exit_swap && !ppt.ppt_name.isExitPoint()) {
        if (combined_exit != null)
          print_invariants_maybe(combined_exit, pw, all_ppts);
        combined_exit = null;
      }

      // Just cache the combined exit point for now, print it after the
      // EXITnn points.
      if (enable_exit_swap && ppt.ppt_name.isCombinedExitPoint()) {
        combined_exit = ppt;
        continue;
      }

      // If there is only one exit point, just show the combined one (since
      // the EXITnn point will be empty)  This is accomplished by skipping this
      // point if it is an EXITnn point and the previous point was a combined
      // exit point and the next one is not an EXITnn point.  But don't skip
      // any conditional ppts attached to the skipped ppt.
      if (enable_exit_swap && (i > 0) && ppt.ppt_name.isExitPoint()) {
        if (ppts[i-1].ppt_name.isCombinedExitPoint()) {
          if (((i + 1) >= ppts.length) || !ppts[i+1].ppt_name.isExitPoint()) {
//             if (Daikon.dkconfig_output_conditionals
//                 && Daikon.output_format == OutputFormat.DAIKON) {
//               for (Iterator<PptConditional> j = ppt.cond_iterator(); j.hasNext() ; ) {
//                 PptConditional pcond = j.next();
//                 print_invariants_maybe(pcond, pw, all_ppts);
//               }
//             }
            continue;
          }
        }
      }

      if (false) {
        VarInfo v = ppt.find_var_by_name ("size(/map.tiles[])");
        System.out.printf ("Found variable %s\n", v);
        if (v != null) {
          List<Invariant> invs = ppt.find_assignment_inv (v);
          System.out.printf ("assignment invs = %s\n", invs);
        }
      }

      print_invariants_maybe(ppt, pw, all_ppts);
    }

    // print a last remaining combined exit point (if any)
    if (enable_exit_swap && combined_exit != null)
      print_invariants_maybe(combined_exit, pw, all_ppts);

    pw.flush();
  }

  /**
   * Print invariants for a single program point and its conditionals.
   * Does no output if no samples or no views.
   **/
  public static void print_invariants_maybe(PptTopLevel ppt,
                                            PrintWriter out,
                                            PptMap all_ppts) {

    debugPrint.fine  ("Considering printing ppt " + ppt.name());

    // Skip this ppt if it doesn't match ppt regular expression
    if ((ppt_regexp != null) && !ppt_regexp.matcher(ppt.name()).find())
      return;

    // Be silent if we never saw any samples.
    // (Maybe this test isn't even necessary, but will be subsumed by others,
    // as all the invariants will be unjustified.)
    if (ppt.num_samples() == 0) {
      if (debugPrint.isLoggable(Level.FINE)) {
        debugPrint.fine ("[No samples for " + ppt.name() + "]");
      }
      if (Daikon.output_num_samples) {
        out.println("[No samples for " + ppt.name() + "]");
      }
      return;
    }
    if ((ppt.numViews() == 0) && (ppt.joiner_view.invs.size() == 0)) {
      if (debugPrint.isLoggable(Level.FINE)) {
        debugPrint.fine ("[No views for " + ppt.name() + "]");
      }
      if (! (ppt instanceof PptConditional)) {
        // Presumably all the views that were originally there were deleted
        // because no invariants remained in any of them.
        if (Daikon.output_num_samples) {
          out.println("[No views for " + ppt.name() + "]");
        }
        return;
      }
    }

    // out.println("This = " + this + ", Name = " + name + " = " + ppt_name);

    if (Daikon.output_format != OutputFormat.IOA) {
      out.println("==========================================="
                  + "================================");
    } else {
      out.println();
      out.println("% Invariants generated by Daikon for");
    }
    print_invariants(ppt, out, all_ppts);

    if (Daikon.dkconfig_output_conditionals
        && Daikon.output_format == OutputFormat.DAIKON) {
      for (Iterator<PptConditional> j = ppt.cond_iterator(); j.hasNext() ; ) {
        PptConditional pcond = j.next();
        print_invariants_maybe(pcond, out, all_ppts);
      }
    }
  }


  /**
   * If Daikon.output_num_samples is enabled, prints the number of samples
   * for the specified ppt.  Also prints all of the variables for the ppt
   * if Daikon.output_num_samples is enabled or the format is ESCJAVA,
   * JML, or DBCJAVA
   */
  public static void print_sample_data(PptTopLevel ppt, PrintWriter out) {

    if (Daikon.output_num_samples) {
      out.println(ppt.name() + "  " + nplural(ppt.num_samples(), "sample"));
    } else {
      if (Daikon.output_format == OutputFormat.IOA) {
        out.print("% ");  // IOA comment style
      }
      out.println(ppt.name());
    }

    // Note that this code puts out the variable list using daikon formatting
    // for the names and not the output specific format.  It also includes
    // both front end and daikon derived variables which are probably not
    // appropriate
    if (Daikon.output_num_samples
        || (Daikon.output_format == OutputFormat.ESCJAVA)
        || (Daikon.output_format == OutputFormat.JML)
        || (Daikon.output_format == OutputFormat.DBCJAVA )) {
      out.print("    Variables:");
      for (int i=0; i<ppt.var_infos.length; i++) {
        if (dkconfig_old_array_names && FileIO.new_decl_format)
          out.print(" " + ppt.var_infos[i].name().replace ("[..]", "[]"));
        else
          out.print(" " + ppt.var_infos[i].name());

      }
      out.println();
    }
  }

  /**
   * prints all variables that were modified if the format is ESCJAVA or
   * DBCJAVA
   */
  public static void print_modified_vars(PptTopLevel ppt, PrintWriter out) {

    debugPrintModified.fine ("Doing print_modified_vars for: " + ppt.name());

    List<VarInfo> modified_vars = new ArrayList<VarInfo>();
    List<VarInfo> reassigned_parameters = new ArrayList<VarInfo>();
    List<VarInfo> unmodified_vars = new ArrayList<VarInfo>();
    List<VarInfo> unmodified_orig_vars = new ArrayList<VarInfo>();

    // Loop through each variable at this ppt
    for (VarInfo vi : ppt.var_infos) {

      // Skip any orig variables
      if (vi.isPrestate()) {
        debugPrintModified.fine ("  skipping " + vi.name()
                                 + ": is prestate");
        continue;
      }
      debugPrintModified.fine ("  Considering var: " + vi.name());

      // Get the orig version of this variable.  If none is found then this
      // isn't a variable about which it makes sense to consider modifiability
      VarInfo vi_orig = ppt.find_var_by_name (vi.prestate_name());
      if (vi_orig == null) {
        debugPrintModified.fine ("  skipping " + vi.name()
                                 + ": no orig variable");
        continue;
      }

      // TODO: When we can get information from the decl file that
      // indicates if a variable is 'final', we should add such a test
      // here.  For now we use the equality invariant between the
      // variable and its orig variable to determine if it has been
      // modified

      if (ppt.is_equal (vi, vi_orig)) {
        debugPrintModified.fine ("  " + vi.name() + " = "
                                 + vi_orig.name());
        unmodified_vars.add (vi);
      } else { // variables are not equal
        if (vi.isParam())
          reassigned_parameters.add (vi);
        else
          modified_vars.add (vi);
      }
    }

    if (Daikon.output_num_samples
        || (Daikon.output_format == OutputFormat.ESCJAVA)
        || (Daikon.output_format == OutputFormat.DBCJAVA)) {
      if (modified_vars.size() > 0) {
        out.print("      Modified variables:");
        for (VarInfo vi : modified_vars)
          out.print(" " + (vi.old_var_name()));
        out.println();
      }
      if (reassigned_parameters.size() > 0) {
        // out.print("      Reassigned parameters:");
        out.print("      Modified primitive arguments:");
        for (VarInfo vi : reassigned_parameters)
          out.print(" " + vi.old_var_name());
        out.println();
      }
      if (unmodified_vars.size() > 0) {
        out.print("      Unmodified variables:");
        for (VarInfo vi : unmodified_vars)
          out.print(" " + vi.old_var_name());
        out.println();
      }
    }

    // Remove non-variables from the assignable output
    if (Daikon.output_format == OutputFormat.ESCJAVA
        || Daikon.output_format == OutputFormat.JML) {
      List<VarInfo> mods = new ArrayList<VarInfo>();
      for (VarInfo vi : modified_vars) {
        if (!vi.is_assignable_var())
          continue;
        mods.add (vi);
      }

      // Print out the modifies/assignable list
      if (mods.size() > 0) {
        if (Daikon.output_format == OutputFormat.ESCJAVA)
          out.print("modifies ");
        else
          out.print("assignable ");
        int inserted = 0;
        for (VarInfo vi : mods) {
          String name = vi.old_var_name();
          if (!name.equals("this")) {
            if (inserted>0) {
              out.print(", ");
            }
            if (name.endsWith("[]")) {
              name = name.substring(0, name.length()-1) + "*]";
            }
            out.print(name);
          inserted++;
          }
        }
        out.println();
      }
    }

  }

  /** Count statistics (via Global) on variables (canonical, missing, etc.) **/
  public static void count_global_stats(PptTopLevel ppt) {
    for (int i=0; i<ppt.var_infos.length; i++) {
      if (ppt.var_infos[i].isDerived()) {
        Global.derived_variables++;
      }
    }
  }

  // This is just a temporary thing to provide more info about the
  // reason invariants are rejected.
  private static String reason = "";

  /** Prints the specified invariant to out. **/
  public static void print_invariant(Invariant inv, PrintWriter out,
                                     int invCounter, PptTopLevel ppt) {
    int inv_num_samps = inv.ppt.num_samples();
    String num_values_samples = "\t\t(" +
      nplural(inv_num_samps, "sample") + ")";

    String inv_rep;
    // All this should turn into simply a call to format_using.
    if (Daikon.output_format == OutputFormat.DAIKON) {
      inv_rep = inv.format_using(Daikon.output_format);
    } else if (Daikon.output_format == OutputFormat.ESCJAVA) {
      if (inv.isValidEscExpression()) {
        inv_rep = inv.format_using(Daikon.output_format);
      } else {
        if (inv instanceof Equality) {
          inv_rep = "warning: method 'equality'.format(OutputFormat:ESC/Java) needs to be implemented: " + inv.format();
        } else {
          inv_rep = "warning: method " + inv.getClass().getName() + ".format(OutputFormat:ESC/Java) needs to be implemented: " + inv.format();
        }
      }
    } else if (Daikon.output_format == OutputFormat.SIMPLIFY) {
      inv_rep = inv.format_using(Daikon.output_format);
    } else if (Daikon.output_format == OutputFormat.IOA) {

      String invName = get_ioa_invname (invCounter, ppt);
      if (debugPrint.isLoggable(Level.FINE)) {
        debugPrint.fine ("Printing normal for " + invName + " with inv " +
                          inv.getClass().getName());
      }

      inv_rep = "invariant " + invName + " of " + ppt.ppt_name.getFullClassName() + ": ";

      inv_rep += get_ioa_precondition (ppt);
      // We look for indexed variables and add fake quantifiers to
      // the left.  Should we be doing this with visitors and the
      // quantification engine?  Maybe, but then again, Daikon
      // doesn't really know what it means to sample.
      String rawOutput = inv.format_using(Daikon.output_format);
      int startPos = rawOutput.indexOf("anIndex");
      if (startPos != -1) {
        int endPos = rawOutput.indexOf ("]", startPos);
        String qvar = rawOutput.substring (startPos, endPos);
        rawOutput = "\\A " + qvar + " (" + rawOutput + ")";
      }
      inv_rep += rawOutput;
      if (PptTopLevel.debug.isLoggable(Level.FINE)) {
        PptTopLevel.debug.fine (inv.repr());
      }
    } else if (Daikon.output_format == OutputFormat.JAVA
               || Daikon.output_format == OutputFormat.JML
               || Daikon.output_format == OutputFormat.DBCJAVA) {

      inv_rep = inv.format_using(Daikon.output_format);

      // TODO: Remove once we revise OutputFormat
      if (Daikon.output_format == OutputFormat.JAVA) {
        inv_rep = inv.format_using (OutputFormat.JAVA);
        // if there is a $pre string in the format, then it contains
        // the orig variable and should not be printed.
        if (inv_rep.indexOf ("$pre") != -1) {
          return;
        }
      }

    } else {
      throw new IllegalStateException("Unknown output mode");
    }
    if (Daikon.output_num_samples) {
      inv_rep += num_values_samples;
    }

    if (debugRepr.isLoggable(Level.FINE)) {
      debugRepr.fine ("Printing: [" + inv.repr_prob() + "]");
    } else if (debugPrint.isLoggable(Level.FINE)) {
      debugPrint.fine ("Printing: [" + inv.repr_prob() + "]");
    }

    if (dkconfig_old_array_names && FileIO.new_decl_format)
      inv_rep = inv_rep.replace ("[..]", "[]");

    if (wrap_xml) {
      out.print("<INVINFO>");
      out.print("<" + inv.ppt.parent.ppt_name.getPoint() + ">");
      out.print("<INV> ");
      out.print(inv_rep);
      out.print(" </INV> ");
      out.print(" <SAMPLES> " + Integer.toString(inv.ppt.num_samples()) + " </SAMPLES> ");
      out.print(" <DAIKON> " + inv.format_using(OutputFormat.DAIKON) + " </DAIKON> ");
      out.print(" <DAIKONCLASS> " + inv.getClass().toString() + " </DAIKONCLASS> ");
      out.print(" <METHOD> " + inv.ppt.parent.ppt_name.getSignature() + " </METHOD> ");
      out.println("</INVINFO>");
    } else {
      out.println(inv_rep);
    }
    if (debug.isLoggable(Level.FINE)) {
      debug.fine (inv.repr());
    }

  }

  /**
   * Takes a list of Invariants and returns a list of Invariants that
   * is sorted according to PptTopLevel.icfp.
   */
  public static List<Invariant> sort_invariant_list(List<Invariant> invs) {
    Invariant[] invs_array = invs.toArray(new Invariant[invs.size()]);
    Arrays.sort(invs_array, PptTopLevel.icfp);

    Vector<Invariant> result = new Vector<Invariant>(invs_array.length);

    for (int i = 0; i < invs_array.length; i++) {
      result.add(invs_array[i]);
    }
    return result;
  }

  /**
   * Print invariants for a single program point, once we know that
   * this ppt is worth printing.
   **/
  public static void print_invariants(PptTopLevel ppt, PrintWriter out,
                                      PptMap ppt_map) {


    // make names easier to read before printing
    ppt.simplify_variable_names();

    print_sample_data(ppt, out);
    print_modified_vars(ppt, out);

    // Dump some debugging info, if enabled
    if (debugPrint.isLoggable(Level.FINE)) {
      debugPrint.fine ("Variables for ppt "  + ppt.name());
      for (int i=0; i<ppt.var_infos.length; i++) {
        VarInfo vi = ppt.var_infos[i];
        PptTopLevel ppt_tl = vi.ppt;
        PptSlice slice1 = ppt_tl.findSlice(vi);
        debugPrint.fine ("      " + vi.name());
      }
      debugPrint.fine ("Equality set: ");
      debugPrint.fine ((ppt.equality_view == null) ? "null"
                       : ppt.equality_view.toString());
    }
    if (debugFiltering.isLoggable(Level.FINE)) {
      debugFiltering.fine ("----------------------------------------"
        + "--------------------------------------------------------");
      debugFiltering.fine (ppt.name());
    }

    // Count statistics (via Global) on variables (canonical, missing, etc.)
    count_global_stats(ppt);

    // I could instead sort the PptSlice objects, then sort the invariants
    // in each PptSlice.  That would be more efficient, but this is
    // probably not a bottleneck anyway.
    List<Invariant> invs_vector = new LinkedList<Invariant>(ppt.getInvariants());

    if (PptSplitter.debug.isLoggable (Level.FINE)) {
      PptSplitter.debug.fine ("Joiner View for ppt " + ppt.name);
      for (Invariant inv : ppt.joiner_view.invs) {
        PptSplitter.debug.fine ("-- " + inv.format());
      }
    }

    if (debugBound.isLoggable (Level.FINE))
      ppt.debug_unary_info (debugBound);

    Invariant[] invs_array = invs_vector.toArray(
      new Invariant[invs_vector.size()]);
    Arrays.sort(invs_array, PptTopLevel.icfp);

    Global.non_falsified_invariants += invs_array.length;

    List<Invariant> accepted_invariants = new Vector<Invariant>();

    for (int i = 0; i < invs_array.length; i++) {
      Invariant inv = invs_array[i];

      if (inv.logOn())
        inv.log ("Considering Printing");
      Assert.assertTrue (!(inv instanceof Equality));
      for (int j = 0; j < inv.ppt.var_infos.length; j++)
        Assert.assertTrue (!inv.ppt.var_infos[j].missingOutOfBounds(),
                           "var '" + inv.ppt.var_infos[j].name()
                            + "' out of bounds in " + inv.format());
      InvariantFilters fi = InvariantFilters.defaultFilters();

      boolean fi_accepted = true;
      InvariantFilter filter_result = null;
      if (!dkconfig_print_all) {
        filter_result = fi.shouldKeep (inv);
        fi_accepted = (filter_result == null);
      }

      if ((inv instanceof Implication)
          && PptSplitter.debug.isLoggable(Level.FINE))
        PptSplitter.debug.fine ("filter result = " + filter_result
                                + " for inv " + inv);

      if (inv.logOn())
        inv.log ("Filtering, accepted = " + fi_accepted);

      // Never print the guarding predicates themselves, they should only
      // print as part of GuardingImplications
      if (fi_accepted && !inv.isGuardingPredicate) {
        Global.reported_invariants++;
        accepted_invariants.add(inv);
      } else {
        if (inv.logOn() || debugPrint.isLoggable(Level.FINE)) {
          inv.log (debugPrint, "fi_accepted = " + fi_accepted +
                    " inv.isGuardingPredicate = " + inv.isGuardingPredicate
                    + " not printing " + inv.repr());
        }
      }
    }

    accepted_invariants
      = InvariantFilters.addEqualityInvariants(accepted_invariants);

    if (debugFiltering.isLoggable(Level.FINE)) {
      for (Invariant current_inv : accepted_invariants) {
        if (current_inv instanceof Equality) {
          debugFiltering.fine ("Found Equality that says "
                                + current_inv.format());
        }
      }
    }

    if (debugFiltering.isLoggable(Level.FINE)) {
      for (int i=0; i<ppt.var_infos.length; i++) {
        VarInfo vi = ppt.var_infos[i];
      }
    }
    finally_print_the_invariants(accepted_invariants, out, ppt);
    if (false && ppt.constants != null)
      ppt.constants.print_missing (out);
  }

  /**
   * Does the actual printing of the invariants.
   **/
  private static void finally_print_the_invariants(List<Invariant> invariants,
                                                   PrintWriter out,
                                                   PptTopLevel ppt) {
    //System.out.printf ("Ppt %s%n", ppt.name());
    //for (VarInfo vi : ppt.var_infos)
    // System.out.printf ("  var %s canbemissing = %b%n", vi, vi.canBeMissing);

    int index = 0;
    for (Invariant inv : invariants) {
      index++;
      Invariant guarded = inv.createGuardedInvariant(false);
      if (guarded != null) {
        inv = guarded;
      }
      print_invariant(inv, out, index, ppt);
    }

    if (dkconfig_replace_prestate) {
      for (Map.Entry<String,String> e : exprToVar.entrySet()) {
        out.println("prestate assignment: " + e.getValue() + "=" + e.getKey());
      }
      resetPrestateExpressions();
    }

  }

  /**
   * Get name of invariant for IOA output, since IOA invariants have
   * to be given unique names.  The name can be derived from a count
   * of the invariants and the program point name.  We simply change
   * the ppt name's characters to be valid IOA syntax.
   **/
  public static String get_ioa_invname (int numbering, PptTopLevel ppt) {
    String replaced = "";
    if (PrintInvariants.test_output) {
      if (ppt.ppt_name.getSignature() != null) {
        replaced = ppt.ppt_name.getSignature().replace
                                                ('(', '_').replace(')', '_');
      }
      return "Inv" + replaced;
    } else {
      if (ppt.ppt_name.getSignature() != null) {
        replaced = ppt.ppt_name.getSignature().replace('(', '_').replace(')', '_');
      }
      return "Inv" + replaced + numbering;
    }
  }

  public static String get_ioa_precondition (PptTopLevel ppt) {
    if (ppt.ppt_name.isClassStaticSynthetic()) return "";
    if (ppt.ppt_name.isObjectInstanceSynthetic()) return "";
    return "enabled(" + ppt.ppt_name.getSignature() + ") => ";
  }

  /**
   * Prints all invariants for ternary slices (organized by slice) and
   * all of the unary and binary invariants over the same variables.
   * The purpose of this is to look for possible ni-suppressions.  Its
   * not intended as a normal output mechanism
   */
  public static void print_all_ternary_invs (PptMap all_ppts) {

    // loop through each ppt
    for (Iterator<PptTopLevel> itor = all_ppts.pptIterator(); itor.hasNext(); ) {
      PptTopLevel ppt = itor.next();

      // if (ppt.num_samples() == 0)
      //  continue;

      // First figure out how many ternary invariants/slices there are
      int lt_cnt = 0;
      int slice_cnt = 0;
      int inv_cnt = 0;
      int total_slice_cnt = 0;
      int total_inv_cnt = 0;
      for (Iterator<PptSlice> si = ppt.views_iterator(); si.hasNext(); ) {
        PptSlice slice = si.next();
        total_slice_cnt++;
        total_inv_cnt += slice.invs.size();
        if (slice.arity() != 3)
          continue;
        slice_cnt++;
        inv_cnt += slice.invs.size();
        for (Invariant inv : slice.invs) {
          if (inv.getClass().getName().indexOf ("Ternary") > 0) {
            lt_cnt++;
          }
        }
      }

      Fmt.pf ("");
      Fmt.pf ("%s - %s samples, %s slices, %s invariants (%s linearternary)",
              ppt.name(),"" + ppt.num_samples(), "" + slice_cnt, "" + inv_cnt,
              "" + lt_cnt);
      Fmt.pf ("    total slice count = " + total_slice_cnt +
              ", total_inv_cnt = " + total_inv_cnt);

      // Loop through each ternary slice
      for (Iterator<PptSlice> si = ppt.views_iterator(); si.hasNext(); ) {
        PptSlice slice = si.next();
        if (slice.arity() != 3)
          continue;
        VarInfo[] vis = slice.var_infos;

        String var_str = "";
        for (int i = 0; i < vis.length; i++) {
          var_str += vis[i].name() + " ";
          if (ppt.is_constant (vis[i]))
            var_str += "["
                 + Debug.toString(ppt.constants.constant_value(vis[i]))+ "] ";
        }
        Fmt.pf ("  Slice %s - %s invariants", var_str, "" + slice.invs.size());

        // Loop through each invariant (skipping ternary ones)
        for (Invariant inv : slice.invs) {
          if (inv.getClass().getName().indexOf ("Ternary") > 0) {
            continue;
          }

          // Check to see if the invariant should be suppressed
          String suppress = "";
          NISuppressionSet ss = inv.get_ni_suppressions();
          if ((ss != null) && ss.suppressed (slice))
            suppress = "ERROR: Should be suppressed by " + ss;

          // Print the invariant
          Fmt.pf ("    %s [%s] %s", inv.format(),
                  UtilMDE.unqualified_name(inv.getClass()), suppress);

          // Print all unary and binary invariants over the same variables
          for (int i = 0; i < vis.length; i++) {
            Fmt.pf ("      %s is %s", vis[i].name(),vis[i].file_rep_type);
            print_all_invs (ppt, vis[i], "      ");
          }
          print_all_invs (ppt, vis[0], vis[1], "      ");
          print_all_invs (ppt, vis[1], vis[2], "      ");
          print_all_invs (ppt, vis[0], vis[2], "      ");
        }
      }
    }
  }

  /**
   * Prints all of the unary invariants over the specified variable
   */
  public static void print_all_invs (PptTopLevel ppt, VarInfo vi,
                                     String indent) {
    String name = Fmt.spf ("%s [%s]", vi.name(), vi.file_rep_type);
    if (ppt.is_missing (vi))
      Fmt.pf ("%s%s missing", indent, name);
    else if (ppt.is_constant (vi))
      Fmt.pf ("%s%s = %s", indent, name,
              Debug.toString(ppt.constants.constant_value(vi)));
    else {
      PptSlice slice = ppt.findSlice (vi);
      if (slice != null)
        print_all_invs (slice, indent);

      if (slice == null)
        Fmt.pf ("%s%s has %s values", indent, name, "" + ppt.num_values (vi));
    }
  }

  /** Prints all of the binary invariants over the specified variables **/
  public static void print_all_invs (PptTopLevel ppt, VarInfo v1, VarInfo v2,
                                     String indent) {
    // Get any invariants in the local slice
    PptSlice slice = ppt.findSlice (v1, v2);
    print_all_invs (slice, indent);

  }

  /** Prints all of the invariants in the specified slice **/
  public static void print_all_invs (PptSlice slice, String indent) {

    if (slice == null)
      return;

    for (Invariant inv : slice.invs) {
      Fmt.pf ("%s%s [%s]", indent, inv.format(),
              UtilMDE.unqualified_name(inv.getClass()));
    }

  }

  /**
   * Prints how many invariants are filtered by each filter
   */
  public static void print_filter_stats (Logger log, PptTopLevel ppt,
                                         PptMap ppt_map) {

    boolean print_invs = false;

    List<Invariant> invs_vector = new LinkedList<Invariant>(ppt.getInvariants());
    Invariant[] invs_array = invs_vector.toArray(
      new Invariant[invs_vector.size()]);

    Map<Class,Map<Class,Integer>> filter_map = new LinkedHashMap<Class,Map<Class,Integer>>();

    if (print_invs)
      debug.fine (ppt.name());

    for (int i = 0; i < invs_array.length; i++) {
      Invariant inv = invs_array[i];

      InvariantFilters fi = InvariantFilters.defaultFilters();
      InvariantFilter filter = fi.shouldKeep(inv);
      Class filter_class = null;
      if (filter != null)
        filter_class = filter.getClass();
      Map<Class,Integer> inv_map = filter_map.get (filter_class);
      if (inv_map == null) {
        inv_map = new LinkedHashMap<Class,Integer>();
        filter_map.put (filter_class, inv_map);
      }
      Integer cnt = inv_map.get (inv.getClass());
      if (cnt == null)
        cnt = new Integer(1);
      else
        cnt = new Integer (cnt.intValue() + 1);
      inv_map.put (inv.getClass(), cnt);

      if (print_invs)
        log.fine (" : " + filter_class + " : " + inv.format());
    }

    log.fine (ppt.name() + ": " + invs_array.length);

    for (Map.Entry<Class,Map<Class,Integer>> entry : filter_map.entrySet()) {
      Class filter_class = entry.getKey();
      Map<Class,Integer> inv_map = entry.getValue();
      int total = 0;
      for (Integer cnt : inv_map.values()) {
        total += cnt.intValue();
      }
      if (filter_class == null)
        log.fine (" : Accepted Invariants : " + total);
      else
        log.fine (" : " + filter_class.getName() + ": " + total);
      for (Map.Entry<Class,Integer> entry2 : inv_map.entrySet()) {
        Class inv_class = entry2.getKey();
        Integer cnt = entry2.getValue();
        log.fine (" : : " + inv_class.getName() + ": " + cnt.intValue());
      }
    }
  }

  public static void print_true_inv_cnt (PptMap ppts) {

    // Count printable invariants
    long inv_cnt = 0;
    for (Iterator<PptTopLevel> i = ppts.pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = i.next();
      for (Invariant inv : ppt.getInvariants()) {
        InvariantFilters fi = InvariantFilters.defaultFilters();
        if (fi.shouldKeep (inv) == null)
          inv_cnt++;
      }
    }
    System.out.printf ("%d printable invariants%n", inv_cnt);

    // Count all of the stored invariants
    inv_cnt = 0;
    for (Iterator<PptTopLevel> i = ppts.pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = i.next();
      inv_cnt += ppt.invariant_cnt();
    }
    System.out.printf ("%d physical invariants%n", inv_cnt);

    //undo suppressions
    for (Iterator<PptTopLevel> i = ppts.pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = i.next();
      NIS.create_suppressed_invs(ppt);
    }

    // Recount with suppressions removed
    inv_cnt = 0;
    for (Iterator<PptTopLevel> i = ppts.pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = i.next();
      inv_cnt += ppt.invariant_cnt();
    }
    System.out.printf ("%d invariants with suppressions removed\n",
                       inv_cnt);

    // Count invariants again, adjusting the count for equality sets
    inv_cnt = 0;
    for (Iterator<PptTopLevel> i = ppts.pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = i.next();
      List<Invariant> invs = ppt.getInvariants();
      for (Invariant inv : invs) {
        int cnt = 1;
        VarInfo[] vis = inv.ppt.var_infos;
        for (VarInfo vi : vis) {
          cnt = cnt * vi.get_equalitySet_size();
        }
        inv_cnt += cnt;
      }
    }
    System.out.printf ("%d invariants with equality removed\n",
                       inv_cnt);

  }
}
