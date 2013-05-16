package daikon.tools.compare;

import java.util.*;
import java.io.*;
import utilMDE.Assert;
import utilMDE.UtilMDE;
import daikon.*;
import daikon.config.Configuration;
import daikon.inv.*;
import daikon.inv.unary.scalar.*;
import daikon.inv.unary.sequence.*;
import daikon.inv.unary.string.*;
import daikon.simplify.*;
import gnu.getopt.*;

/**
 * This is a standalone program that compares the invariants from two
 * versions of (and/or runs of) a program, and determines using
 * Simplify whether the invariants from one logically imply the
 * invariants from the other. These are referred to below as the
 * "test" and "application" invariants, and the conditions that are
 * checked is that the each test precondition (ENTER point invariant)
 * must be implied some combination of application preconditions, and
 * that each application postcondition (EXIT point invariant) must be
 * implied by some combination of test postconditions and application
 * preconditions.
 **/

public class LogicalCompare {
  private LogicalCompare() { throw new Error("do not instantiate"); }

  // Options corresponding to command-line flags
  private static boolean opt_proofs         = false;
  private static boolean opt_show_count     = false;
  private static boolean opt_show_formulas  = false;
  private static boolean opt_show_valid     = false;
  private static boolean opt_post_after_pre = true;
  private static boolean opt_timing         = false;
  private static boolean opt_show_sets      = false;
  private static boolean opt_minimize_classes = false;

  private static Map<String,Vector<Lemma>> extra_assumptions;

  private static LemmaStack lemmas;

  private static String usage =
    UtilMDE.joinLines(
      "Usage: java daikon.tools.compare.LogicalCompare [options ...]",
      "           WEAK-INVS STRONG-INVS [ENTER-PPT [EXIT-PPT]]",
      "  -h, --help",
      "      Display this usage message",
      "  --config-file FILE",
      "      Read configuration option file",
      "  --cfg OPTION=VALUE",
      "      Set individual configuration option",
      "  --debug-all",
      "      Enable all debugging logs",
      "  --dbg CATEGORY",
      "      Enable a single category of debug log",
      "  --proofs",
      "      Show minimal sufficient conditions for valid invariants",
      "  --show-count",
      "      Print count of invariants checked",
      "  --show-formulas",
      "      Print Simplify representation of invariants",
      "  --show-valid",
      "      Show invariants that are verified as well as those that fail",
      "  --show-sets",
      "      Show, not test, all the invariants that would be considered",
      "  --post-after-pre-failure",
      "      Check postcondition match even if preconditions fail",
      "  --no-post-after-pre-failure",
      "      Don't check postcondition match if preconditions fail",
      "  --timing",
      "      Show time required to check invariants",
      "  --filters [bBoOmjpis]",
      "      Control which invariants are removed from consideration",
      "  --assume FILE",
      "      Read extra assumptions from FILE"
      );

  // Filter options
  // b        discard uninteresting-constant bounds
  // B        discard all bounds
  // o        discard uninteresting-constant one-ofs
  // O        discard all one-ofs
  // m        discard invariants with only missing samples
  // j        discard statistically unjustified invariants
  // p        discard invariants over pass-by-value parameters
  // i        discard implication pre-conditions
  // s        discard suppressed invariants

  private static boolean[] filters = new boolean[128];

  private static Vector<Invariant> filterInvariants(Vector<Invariant> invs, boolean isPost) {
    Vector<Invariant> new_invs = new Vector<Invariant>();
    for (Invariant inv : invs) {
      Invariant guarded_inv = inv;
//       System.err.println("Examining " + inv.format());
      if (inv instanceof GuardingImplication)
        inv = ((Implication)inv).consequent();
      if (inv instanceof LowerBound || inv instanceof UpperBound ||
          inv instanceof EltLowerBound || inv instanceof EltUpperBound ||
          inv instanceof LowerBoundFloat || inv instanceof UpperBoundFloat ||
          inv instanceof EltLowerBoundFloat ||
          inv instanceof EltUpperBoundFloat) {
        if (filters['b'] && inv.hasUninterestingConstant())
          continue;
        if (filters['B'])
          continue;
      }
      if (inv instanceof OneOf || inv instanceof OneOfString ||
          inv instanceof OneOfFloat) {
        if (filters['o'] && inv.hasUninterestingConstant())
          continue;
        if (filters['O'])
          continue;
      }
      // test used to be "(filters['m'] && inv.ppt.num_mod_samples() == 0)"
      if (filters['m'] && inv.ppt.num_samples() == 0)
        continue;
      if (filters['j'] && !inv.justified())
        continue;
      if (filters['p'] && isPost && shouldDiscardInvariant(inv))
        continue;
      if (filters['i'] && !isPost && inv instanceof Implication)
        continue;
      String simp = inv.format_using(OutputFormat.SIMPLIFY);
      if (simp.indexOf("format_simplify") != -1 ||
          simp.indexOf("OutputFormat:Simplify") != -1 ||
          simp.indexOf("Simplify not implemented") != -1) {
        // Noisy, since we should be able to handle most invariants now
        System.out.println("Can't handle " + inv.format() + ": " + simp);
        continue;
      }
      if (inv.format_using(OutputFormat.DAIKON)
          .indexOf("warning: too few samples") != -1)
        continue;
      if (inv.isGuardingPredicate)
        continue;
//       System.err.println("Keeping   " + inv.format());
      new_invs.add(guarded_inv);
    }
    return new_invs;
  }

  // This is a modified version of the method with the same name from
  // the DerivedParameterFilter, with an exception for invariants
  // indicating that variables are unchanged (except that that
  // exception is currently turned off)
  private static boolean shouldDiscardInvariant( Invariant inv ) {
    for (int i = 0; i < inv.ppt.var_infos.length; i++) {
      VarInfo vi = inv.ppt.var_infos[i];
      // ppt has to be a PptSlice, not a PptTopLevel
      if (vi.isDerivedParamAndUninteresting()) {
        // Exception: let invariants like "orig(arg) == arg" through.
        if (IsEqualityComparison.it.accept( inv )) {
          Comparison comp = (Comparison)inv;
          VarInfo var1 = comp.var1();
          VarInfo var2 = comp.var2();
          boolean vars_are_same = var1.prestate_name().equals (var2.name())
            || var2.prestate_name().equals (var1.name());
          if (vars_are_same) return false;
        }
//         if (inv instanceof OneOf || inv instanceof OneOfString ||
//             inv instanceof OneOfString)
//           return false;
//         System.err.println("Because of " + vi.name.name() + ", discarding "
//                            + inv.format());
        return true;
      }
    }
    return false;
  }

  // Translate a vector of Invariants into a vector of Lemmas, without
  // changing the invariants.
  private static Vector<Lemma> translateStraight(Vector<Invariant> invs) {
    Vector<Lemma> lems = new Vector<Lemma>();
    for (Invariant inv : invs) {
      lems.add(new InvariantLemma(inv));
    }
    return lems;
  }

  // Translate a vector of Invariants into a vector of Lemmas,
  // discarding any invariants that represent only facts about a
  // routine's prestate.
  private static Vector<Lemma> translateRemovePre(Vector<Invariant> invs) {
    Vector<Lemma> lems = new Vector<Lemma>();
    for (Invariant inv : invs) {
      if (!inv.isAllPrestate())
        lems.add(new InvariantLemma(inv));
    }
    return lems;
  }

  // Translate a vector of Invariants into a vector of Lemmas, adding
  // orig(...) to each variable so that the invariant will be true
  // about the precondition of a routine when it is examined in the
  // poststate context.
  private static Vector<Lemma> translateAddOrig(Vector<Invariant> invs) {
    Vector<Lemma> lems = new Vector<Lemma>();
    for (Invariant inv : invs) {
      lems.add(InvariantLemma.makeLemmaAddOrig(inv));
    }
    return lems;
  }

//   // Print a vector of invariants and their Simplify translations, for
//   // debugging purposes.
//   private static void printInvariants(Vector<Invariant> invs) {
//     for (Invariant inv : invs) {
//       System.out.println("   " + inv.format());
//       System.out.println("(BG_PUSH "
//                          + inv.format_using(OutputFormat.SIMPLIFY) +")");
//     }
//   }

  private static String shortName(Class c) {
    String name = c.getName();
    return name.substring(name.lastIndexOf('.') + 1);
  }

  private static int checkConsequences(Vector<Lemma> assumptions, Vector<Lemma> consequences) {
    Set<String> assumption_formulas = new HashSet<String>();
    for (Lemma lem : assumptions) {
      assumption_formulas.add(lem.formula);
    }

    int invalidCount = 0;
    for (Lemma inv : consequences) {
      char result;
      boolean identical = false;
      if (assumption_formulas.contains(inv.formula)) {
        result = 'T';
        identical = true;
      } else {
        result = lemmas.checkLemma(inv);
      }

      if (opt_minimize_classes) {
        if (result == 'T' && !identical) {
          Vector<Set<Class>> sets = lemmas.minimizeClasses(inv.formula);
          for (Set<Class> classes : sets) {
            Class inv_class = inv.invClass();
            System.out.print(shortName(inv_class) + ":");
            if (classes.contains(inv_class)) {
              System.out.print(" " + shortName(inv_class));
              classes.remove(inv_class);
            }
            for (Class c : classes) {
              System.out.print(" " + shortName(c));
            }
            System.out.println();
          }
          System.out.println(inv.summarize());
          System.out.println();
        }
        if (true)
          continue;
      }

      if (result == 'T') {
        if (opt_proofs) {
          if (identical) {
            System.out.println("Identical");
          } else {
            Vector<Lemma> assume = lemmas.minimizeProof(inv);
            System.out.println();
            for (Lemma lem : assume)
              System.out.println(lem.summarize());
            System.out.println("----------------------------------");
            System.out.println(inv.summarize());
            if (opt_show_formulas) {
              System.out.println();
              for (Lemma lem : assume)
                System.out.println("    "  + lem.formula);
              System.out.println("    ----------------------"
                                 + "--------------------");
              System.out.println("    " + inv.formula);
            }
          }
        } else if (opt_show_valid) {
          System.out.print("Valid: ");
          System.out.println(inv.summary);
          if (opt_show_formulas)
            System.out.println("    " + inv.formula);
        }
      } else if (result == 'F') {
        invalidCount++;
        if (opt_proofs)
          System.out.println();
        System.out.print("Invalid: ");
        System.out.println(inv.summary);
        if (opt_show_formulas)
          System.out.println("    " + inv.formula);
      } else {
        Assert.assertTrue(result == '?');
        if (opt_proofs)
          System.out.println();
        System.out.print("Timeout: ");
        System.out.println(inv.summary);
        if (opt_show_formulas)
          System.out.println("    " + inv.formula);
      }
    }
    return invalidCount;
  }


  // Check that each of the invariants in CONSEQUENCES follows from
  // zero or more of the invariants in ASSUMPTIONS. Returns the number
  // of invariants that can't be proven to follow.
  private static int evaluateImplications(Vector<Lemma> assumptions,
                                          Vector<Lemma> consequences)
  throws SimplifyError {
    int mark = lemmas.markLevel();
    lemmas.pushOrdering();
    lemmas.pushLemmas(assumptions);
    if (lemmas.checkForContradiction() == 'T') {
      if (opt_post_after_pre) {
        // Shouldn't be reached anymore
        lemmas.removeContradiction();
        System.out.println("Warning: had to remove contradiction(s)");
      } else {
        System.out.println("Contradictory assumptions:");
        Vector<Lemma> min = lemmas.minimizeContradiction();
        LemmaStack.printLemmas(System.out, min);
        Assert.assertTrue(false, "Aborting");
      }
    }

    int invalidCount = checkConsequences(assumptions, consequences);

    lemmas.popToMark(mark);
    return invalidCount;
  }

  private static int evaluateImplicationsCarefully(Vector<Lemma> safeAssumptions,
                                                   Vector<Lemma> unsafeAssumptions,
                                                   Vector<Lemma> consequences)
    throws SimplifyError {
    int mark = lemmas.markLevel();
    Vector<Lemma> assumptions = new Vector<Lemma>();
    lemmas.pushOrdering();
    lemmas.pushLemmas(safeAssumptions);
    if (lemmas.checkForContradiction() == 'T') {
      System.out.println("Contradictory assumptions:");
      Vector<Lemma> min = lemmas.minimizeContradiction();
      LemmaStack.printLemmas(System.out, min);
      Assert.assertTrue(false, "Aborting");
    }
    assumptions.addAll(safeAssumptions);

    int j = unsafeAssumptions.size();
    for (int i = 0; i < unsafeAssumptions.size(); i++) {
      List<Lemma> unsafe = unsafeAssumptions.subList(i, j);
      boolean safe = false;
      while (!safe && unsafe.size() > 0) {
        int innerMark = lemmas.markLevel();
        lemmas.pushLemmas(new Vector<Lemma>(unsafe));
        if (lemmas.checkForContradiction() == 'T') {
          lemmas.popToMark(innerMark);
          j = i + unsafe.size();
          unsafe = unsafe.subList(0, unsafe.size() / 2);
        } else {
          safe = true;
          i += unsafe.size() - 1;
          assumptions.addAll(unsafe);
        }
      }
      if (!safe) {
        Assert.assertTrue(unsafe.size() == 0);
        j = unsafeAssumptions.size();
      }
    }

    int invalidCount = checkConsequences(assumptions, consequences);

    lemmas.popToMark(mark);
    return invalidCount;
  }


  // Initialize the theorem prover. Whichever mode we're in, we should
  // only do this once per program run.
  private static void startProver() {
    lemmas = new LemmaStack();
  }

  // Comparare the invariants for enter and exit points between two
  // methods (usually two sets of invariants for methods of the same
  // name)
  private static void comparePpts(PptTopLevel app_enter_ppt,
                                  PptTopLevel test_enter_ppt,
                                  PptTopLevel app_exit_ppt,
                                  PptTopLevel test_exit_ppt) {
    LemmaStack.clearInts();

//     app_enter_ppt.guardInvariants();
//     test_enter_ppt.guardInvariants();
//     app_exit_ppt.guardInvariants();
//     test_exit_ppt.guardInvariants();

    Vector<Invariant> a_pre = app_enter_ppt.invariants_vector();
    Vector<Invariant> t_pre = test_enter_ppt.invariants_vector();
    Vector<Invariant> a_post = app_exit_ppt.invariants_vector();
    Vector<Invariant> t_post = test_exit_ppt.invariants_vector();

    if (opt_timing)
      System.out.println("Starting timer");
    long processing_time_start = System.currentTimeMillis();

    a_pre  = filterInvariants(a_pre, false);
    t_pre  = filterInvariants(t_pre, false);
    a_post = filterInvariants(a_post, true);
    t_post = filterInvariants(t_post, true);

    if (opt_show_sets) {
      System.out.println("Background assumptions:");
      LemmaStack.printLemmas(System.out, Lemma.lemmasVector());
      System.out.println("");

      Vector<Lemma> v = new Vector<Lemma>();
      v.addAll(translateAddOrig(a_pre));
      System.out.println("Weak preconditions (Apre):");
      LemmaStack.printLemmas(System.out, v);
      System.out.println("");

      v = new Vector<Lemma>();
      v.addAll(translateAddOrig(t_pre));
      System.out.println("Strong preconditions (Tpre):");
      LemmaStack.printLemmas(System.out, v);
      System.out.println("");

      v = new Vector<Lemma>();
      v.addAll(translateRemovePre(t_post));
      System.out.println("Strong postconditions (Tpost):");
      LemmaStack.printLemmas(System.out, v);
      System.out.println("");

      v = new Vector<Lemma>();
      v.addAll(translateRemovePre(a_post));
      System.out.println("Weak postconditions (Apost):");
      LemmaStack.printLemmas(System.out, v);
      return;
    }

    if (opt_show_count)
      System.out.println("Strong preconditions consist of "
                         + t_pre.size() + " invariants.");
    Vector<Lemma> pre_assumptions = new Vector<Lemma>();
    pre_assumptions.addAll(translateStraight(a_pre));
    Vector<Lemma> pre_conclusions = new Vector<Lemma>();
    pre_conclusions.addAll(translateStraight(t_pre));
    Collections.sort(pre_conclusions);

    int bad_pre = evaluateImplications(pre_assumptions, pre_conclusions);
    int num_checked = pre_conclusions.size();
    if (bad_pre > 0 && !opt_post_after_pre) {
      System.out.println("Precondition failure, skipping postconditions");
      return;
    }

    System.out.println("================================================="
                       + "=============================");


    Vector<Lemma> post_assumptions_safe = new Vector<Lemma>();
    Vector<Lemma> post_assumptions_unsafe = new Vector<Lemma>();
    Vector<Lemma> post_conclusions = new Vector<Lemma>();

    post_assumptions_unsafe.addAll(translateAddOrig(a_pre));
    post_assumptions_safe.addAll(translateStraight(t_post));
    String ppt_name = test_enter_ppt.ppt_name.name();
    if (extra_assumptions.containsKey(ppt_name)) {
      Vector<Lemma> assumptions = extra_assumptions.get(ppt_name);
      post_assumptions_unsafe.addAll(assumptions);
    }

    post_conclusions.addAll(translateRemovePre(a_post));
    Collections.sort(post_conclusions);

    if (opt_show_count)
      System.out.println("Weak postconditions consist of "
                         + post_conclusions.size() + " invariants.");

    evaluateImplicationsCarefully(post_assumptions_safe,
                                  post_assumptions_unsafe, post_conclusions);
    long time_elapsed = System.currentTimeMillis() - processing_time_start;
    num_checked += post_conclusions.size();
    if (opt_show_count)
      System.out.println("Checked " + num_checked + " invariants total");
    if (opt_timing)
      System.out.println("Total time " + time_elapsed + "ms");
  }

  private static void readExtraAssumptions(String filename) {
    File file = new File(filename);
    try {
      LineNumberReader reader = UtilMDE.lineNumberFileReader(file);
      String line;
      String ppt_name = null;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.equals("") || line.startsWith("#")) {
          continue;
        } else if (line.startsWith("PPT_NAME")) {
          ppt_name = line.substring("PPT_NAME".length()).trim();
          if (!extra_assumptions.containsKey(ppt_name)) {
            extra_assumptions.put(ppt_name, new Vector<Lemma>());
          }
        } else if (line.startsWith("(")) {
          if (ppt_name == null) {
            System.err.println("Must specify PPT_NAME before " +
                               "giving a formula");
            Assert.assertTrue(false);
          }
          String formula, comment;
          // XXX This should really read a balanced Simplify
          // expression, then look for a comment after that. But that
          // would involve counting parens and vertical bars and
          // backslashes, which I'm too lazy to do right now.
          if (line.indexOf("#") != -1) {
            formula = line.substring(0, line.indexOf("#"));
            comment = line.substring(line.indexOf("#") + 1);
          } else {
            formula = line;
            comment = "User-supplied assumption";
          }
          formula = formula.trim();
          comment = comment.trim();
          Vector<Lemma> assumption_vec = extra_assumptions.get(ppt_name);
          assumption_vec.add(new Lemma(comment, formula));
        } else {
          System.err.println("Can't parse " + line + " in assumptions file");
          System.err.println("Should be `PPT_NAME <ppt_name>' or a Simplify " +
                             "formula (starting with `(')");
          Assert.assertTrue(false);
        }
      }
    } catch (FileNotFoundException e) {
      System.err.println("File not found: " + filename);
      Assert.assertTrue(false);
    } catch (IOException e) {
      System.err.println("IO error reading " + filename);
      Assert.assertTrue(false);
    }
  }

  public static void main(String[] args)
    throws FileNotFoundException, IOException, SimplifyError
  {
    try {
      mainHelper(args);
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
  public static void mainHelper(final String[] args)
    throws FileNotFoundException, IOException, SimplifyError
  {
    LongOpt[] longopts = new LongOpt[] {
      new LongOpt("assume",              LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt("cfg",                 LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt("config-file",         LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt("dbg",                 LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt("debug-all",                 LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt("filters",             LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt("help",                      LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt("minimize-classes"      ,    LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt("no-post-after-pre-failure", LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt("post-after-pre-failure",    LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt("proofs",                    LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt("show-count",                LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt("show-formulas",             LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt("show-sets",                 LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt("show-valid",                LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt("timing",                    LongOpt.NO_ARGUMENT, null, 0),
    };

    Configuration.getInstance().apply("daikon.inv.Invariant." +
                                      "simplify_define_predicates=true");
    Configuration.getInstance().apply("daikon.simplify.Session." +
                                      "simplify_max_iterations=2147483647");

    extra_assumptions = new LinkedHashMap<String,Vector<Lemma>>();

    Getopt g = new Getopt("daikon.tools.compare.LogicalCompare", args, "h",
                          longopts);
    int c;
    boolean user_filters = false;
    while ((c = g.getopt()) != -1) {
      switch(c) {
      case 0:
        // got a long option
        String option_name = longopts[g.getLongind()].getName();
        if (option_name.equals("help")) {
          System.out.println(usage);
          throw new Daikon.TerminationMessage();
        } else if (option_name.equals("config-file")) {
          String config_file = g.getOptarg();
          try {
            InputStream stream = new FileInputStream(config_file);
            Configuration.getInstance().apply(stream);
          } catch (IOException e) {
            throw new RuntimeException("Could not open config file "
                                       + config_file);
          }
        } else if (option_name.equals("cfg")) {
          String item = g.getOptarg();
          Configuration.getInstance().apply(item);
        } else if (option_name.equals("debug")) {
          Global.debugAll = true;
        } else if (option_name.equals("dbg")) {
          LogHelper.setLevel(g.getOptarg(), LogHelper.FINE);
        } else if (option_name.equals("proofs")) {
          opt_proofs = true;
        } else if (option_name.equals("show-count")) {
          opt_show_count = true;
        } else if (option_name.equals("show-formulas")) {
          opt_show_formulas = true;
        } else if (option_name.equals("show-valid")) {
          opt_show_valid = true;
        } else if (option_name.equals("show-sets")) {
          opt_show_sets = true;
        } else if (option_name.equals("post-after-pre-failure")) {
          opt_post_after_pre = true;
        } else if (option_name.equals("no-post-after-pre-failure")) {
          opt_post_after_pre = false;
        } else if (option_name.equals("timing")) {
          opt_timing = true;
        } else if (option_name.equals("filters")) {
          String f = g.getOptarg();
          for (int i = 0; i < f.length(); i++) {
            filters[f.charAt(i)] = true;
          }
          user_filters = true;
        } else if (option_name.equals("assume")) {
          readExtraAssumptions(g.getOptarg());
        } else if (option_name.equals("minimize-classes")) {
          opt_minimize_classes = true;
        } else {
          Assert.assertTrue(false);
        }
        break;
      case 'h':
        System.out.println(usage);
        throw new Daikon.TerminationMessage();
      case '?':
        // getopt() already printed an error
        throw new Daikon.TerminationMessage("Bad argument");
      }
    }

    if (!user_filters) {
      filters['i'] = filters['j'] = filters['m'] = filters['p'] = true;
    }

    LogHelper.setupLogs(Global.debugAll ? LogHelper.FINE : LogHelper.INFO);

    int num_args = args.length - g.getOptind();

    if (num_args < 2) {
      throw new Daikon.TerminationMessage("Must have at least two non-option arguments", usage);
    }

    String app_filename = args[g.getOptind() + 0];
    String test_filename = args[g.getOptind() + 1];

    PptMap app_ppts = FileIO.read_serialized_pptmap(new File(app_filename),
                                                    true // use saved config
                                                    );
    PptMap test_ppts = FileIO.read_serialized_pptmap(new File(test_filename),
                                                     true // use saved config
                                                     );
    if (num_args == 4 || num_args == 3) {
      String enter_ppt_name = args[g.getOptind() + 2];
      String exit_ppt_name;
      if (num_args == 4)
        exit_ppt_name = args[g.getOptind() + 3];
      else
        exit_ppt_name = ""; // s/b nonexistent
      startProver();

      System.out.println("Comparing " + enter_ppt_name + " and "
                         + exit_ppt_name + " in " + app_filename
                         + " and " + test_filename);

      PptTopLevel app_enter_ppt = app_ppts.get(enter_ppt_name);
      PptTopLevel test_enter_ppt = test_ppts.get(enter_ppt_name);
      Assert.assertTrue(app_enter_ppt != null);
      Assert.assertTrue(test_enter_ppt != null);

      PptTopLevel app_exit_ppt = app_ppts.get(exit_ppt_name);
      PptTopLevel test_exit_ppt = test_ppts.get(exit_ppt_name);
      if (app_exit_ppt == null)
        app_exit_ppt = app_ppts.get(app_enter_ppt.ppt_name.makeExit());
      if (test_exit_ppt == null)
        test_exit_ppt = test_ppts.get(test_enter_ppt.ppt_name.makeExit());

      Assert.assertTrue(app_exit_ppt != null);
      Assert.assertTrue(test_exit_ppt != null);
      comparePpts(app_enter_ppt, test_enter_ppt,
                  app_exit_ppt, test_exit_ppt);
    } else if (num_args == 2) {
      startProver();

      Collection<String> app_ppt_names = app_ppts.nameStringSet();
      Collection<String> test_ppt_names = test_ppts.nameStringSet();
      Set<String> common_names = new TreeSet<String>();

      for (String name : app_ppt_names) {
        PptTopLevel app_ppt = app_ppts.get(name);

        if (!app_ppt.ppt_name.isEnterPoint()) {
          // an exit point, and we only want entries
          continue;
        }
        if (app_ppt.num_samples() > 0) {
          if (test_ppt_names.contains(name)
              && test_ppts.get(name).num_samples() > 0) {
            common_names.add(name);
          } else {
            System.out.println(name + " was used but not tested");
          }
        }
      }

      for (String name : common_names) {
        System.out.println("Looking at " + name);
        PptTopLevel app_enter_ppt = app_ppts.get(name);
        PptTopLevel test_enter_ppt = test_ppts.get(name);
        PptTopLevel app_exit_ppt =
          app_ppts.get(app_enter_ppt.ppt_name.makeExit());
        PptTopLevel test_exit_ppt =
          test_ppts.get(test_enter_ppt.ppt_name.makeExit());

        Assert.assertTrue(app_exit_ppt != null);
        Assert.assertTrue(test_exit_ppt != null);
        comparePpts(app_enter_ppt, test_enter_ppt,
                    app_exit_ppt, test_exit_ppt);

      }
    } else {
      throw new Daikon.TerminationMessage("Too many arguments", usage);
    }
  }
}
