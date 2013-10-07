package daikon.tools;

import java.util.*;
import java.io.*;
import java.util.logging.Logger;
import java.util.regex.*;
import gnu.getopt.*;
import utilMDE.UtilMDE;
import daikon.*;
import daikon.inv.*;

/**
 * Extract the consequents of all Implication invariants that are predicated
 * by membership in a cluster, from a .inv file.  An example of such an
 * implication would be "(cluster == <NUM>) ==> consequent". The consequent
 * is only true in certain clusters, but is not generally true for all
 * executions of the program point to which the Implication belongs.  These
 * resulting implications are written to standard output in the format of a
 * splitter info file.
 **/
public class ExtractConsequent {

  public static final Logger debug = Logger.getLogger ("daikon.ExtractConsequent");
  private static final String lineSep = Global.lineSep;

  private static class HashedConsequent {
    Invariant inv;

    // We prefer "x < y", "x > y", and "x == y" to the conditions
    // "x >= y", "x <= y", and "x != y" that (respectively) give the
    // same split.  When we see a dispreferred form, we index it by
    // the preferred form, and if there's already an entry (from the
    // real preferred one) we throw the new one out. Otherwise, we
    // insert both the dispreferred form and an entry for the
    // preferred form, with a pointer pack to the dispreferred
    // form. If we later see the preferred form, we replace the
    // placeholder and remove the dispreferred form.
    String fakeFor;

    HashedConsequent(Invariant i, String ff) {
      inv = i;
      fakeFor = ff;
    }
  }

  /* A HashMap whose keys are PPT names (Strings) and whose values are
      HashMaps whose keys are predicate names (Strings) and whose values are
       HashMaps whose keys are Strings (normalized java-format invariants)
         and whose values are HashedConsequent objects. */
  private static Map<String,Map<String,Map<String,HashedConsequent>>> pptname_to_conditions = new HashMap<String,Map<String,Map<String,HashedConsequent>>>();

  private static String usage =
    UtilMDE.joinLines(
      "Usage: java daikon.ExtractConsequent [OPTION]... FILE",
      "  -h, --" + Daikon.help_SWITCH,
      "      Display this usage message",
      "  --" + Daikon.suppress_redundant_SWITCH,
      "      Suppress display of logically redundant invariants.",
      "  --" + Daikon.debugAll_SWITCH,
      "      Turn on all debug switches",
      "  --" + Daikon.debug_SWITCH + " <logger>",
      "      Turn on the specified debug logger");


  public static void main(String[] args)
    throws FileNotFoundException, IOException, ClassNotFoundException
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
    throws FileNotFoundException, IOException, ClassNotFoundException
  {
    daikon.LogHelper.setupLogs(daikon.LogHelper.INFO);
    LongOpt[] longopts = new LongOpt[] {
      new LongOpt(Daikon.suppress_redundant_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.config_option_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
      new LongOpt(Daikon.debugAll_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.debug_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
    };
    Getopt g = new Getopt("daikon.ExtractConsequent", args, "h", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch (c) {
      case 0:
        // got a long option
        String option_name = longopts[g.getLongind()].getName();
        if (Daikon.help_SWITCH.equals(option_name)) {
          System.out.println(usage);
          throw new Daikon.TerminationMessage();
        } else if (Daikon.suppress_redundant_SWITCH.equals(option_name)) {
          Daikon.suppress_redundant_invariants_with_simplify = true;
        } else if (Daikon.config_option_SWITCH.equals(option_name)) {
          String item = g.getOptarg();
          daikon.config.Configuration.getInstance().apply(item);
          break;
        } else if (Daikon.debugAll_SWITCH.equals(option_name)) {
          Global.debugAll = true;
        } else if (Daikon.debug_SWITCH.equals(option_name)) {
          LogHelper.setLevel(g.getOptarg(), LogHelper.FINE);
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
    // The index of the first non-option argument -- the name of the file
    int fileIndex = g.getOptind();
    if (args.length - fileIndex != 1) {
      throw new Daikon.TerminationMessage("Wrong number of arguments.", usage);
    }
    String filename = args[fileIndex];
    PptMap ppts = FileIO.read_serialized_pptmap(new File(filename),
                                                true // use saved config
                                                );
    extract_consequent(ppts);
  }

  public static void extract_consequent(PptMap ppts) {
    // Retrieve Ppt objects in sorted order.
    // Use a custom comparator for a specific ordering
    Comparator<PptTopLevel> comparator = new Ppt.NameComparator();
    TreeSet<PptTopLevel> ppts_sorted = new TreeSet<PptTopLevel>(comparator);
    ppts_sorted.addAll(ppts.asCollection());

    for (PptTopLevel ppt : ppts_sorted) {
      extract_consequent_maybe(ppt, ppts);
    }

    PrintWriter pw = new PrintWriter(System.out, true);

    // All conditions at a program point.  A TreeSet to enable
    // deterministic output.
    TreeSet<String> allConds = new TreeSet<String>();
    for ( String pptname : pptname_to_conditions.keySet() ) {
      Map<String,Map<String,HashedConsequent>> cluster_to_conditions = pptname_to_conditions.get(pptname);
      for ( Map.Entry<String,Map<String,HashedConsequent>> entry : cluster_to_conditions.entrySet()) {
        String predicate = entry.getKey();
        Map<String,HashedConsequent> conditions = entry.getValue();
        StringBuffer conjunctionJava = new StringBuffer();
        StringBuffer conjunctionDaikon = new StringBuffer();
        StringBuffer conjunctionIOA = new StringBuffer();
        StringBuffer conjunctionESC = new StringBuffer();
        StringBuffer conjunctionSimplify = new StringBuffer("(AND ");
        int count = 0;
        for (Map.Entry<String,HashedConsequent> entry2 : conditions.entrySet()) {
          count++;
          String condIndex = entry2.getKey();
          HashedConsequent cond = entry2.getValue();
          if (cond.fakeFor != null) {
            count--;
            continue;
          }
          String javaStr = cond.inv.format_using(OutputFormat.JAVA);
          String daikonStr = cond.inv.format_using(OutputFormat.DAIKON);
          String ioaStr = cond.inv.format_using(OutputFormat.IOA);
          String escStr = cond.inv.format_using(OutputFormat.ESCJAVA);
          String simplifyStr = cond.inv.format_using(OutputFormat.SIMPLIFY);
          allConds.add(combineDummy(condIndex, "<dummy> " + daikonStr,
                                    ioaStr, escStr, simplifyStr));
//           allConds.add(condIndex);
          if (count > 0) {
            conjunctionJava.append(" && ");
            conjunctionDaikon.append(" and ");
            conjunctionIOA.append(" /\\ ");
            conjunctionESC.append(" && ");
            conjunctionSimplify.append(" ");
          }
          conjunctionJava.append(javaStr);
          conjunctionDaikon.append(daikonStr);
          conjunctionIOA.append(ioaStr);
          conjunctionESC.append(escStr);
          conjunctionSimplify.append(simplifyStr);
        }
        conjunctionSimplify.append(")");
        String conj = conjunctionJava.toString();
        // Avoid inserting self-contradictory conditions such as "x == 1 &&
        // x == 2", or conjunctions of only a single condition.
        if (count < 2
            || contradict_inv_pattern.matcher(conj).find()
            || useless_inv_pattern_1.matcher(conj).find()
            || useless_inv_pattern_2.matcher(conj).find()) {
          // System.out.println("Suppressing: " + conj);
        } else {
          allConds.add(combineDummy(conjunctionJava.toString(),
                                    conjunctionDaikon.toString(),
                                    conjunctionIOA.toString(),
                                    conjunctionESC.toString(),
                                    conjunctionSimplify.toString()));
        }
      }

      if (allConds.size() > 0) {
        pw.println();
        pw.println("PPT_NAME " + pptname);
        for (String s : allConds) {
          pw.println(s);
        }
      }
      allConds.clear();
    }

    pw.flush();
  }

  static String combineDummy(String inv, String daikonStr, String ioa, String esc,
                             String simplify) {
    StringBuffer combined = new StringBuffer(inv);
    combined.append(lineSep + "\tDAIKON_FORMAT ");
    combined.append(daikonStr);
    combined.append(lineSep + "\tIOA_FORMAT ");
    combined.append(ioa);
    combined.append(lineSep + "\tESC_FORMAT ");
    combined.append(esc);
    combined.append(lineSep + "\tSIMPLIFY_FORMAT ");
    combined.append(simplify);
    return combined.toString();
  }


  /**
   * Extract consequents from a implications at a single program
   * point. It only searches for top level Program points because
   * Implications are produced only at those points.
   **/
  public static void extract_consequent_maybe(PptTopLevel ppt,
                                              PptMap all_ppts) {
    ppt.simplify_variable_names();

    Invariants invs = new Invariants();
    if (invs.size() > 0) {
      String pptname = cleanup_pptname(ppt.name());
      for (Invariant maybe_as_inv : invs) {
        Implication maybe = (Implication)maybe_as_inv;

        // don't print redundant invariants.
        if (Daikon.suppress_redundant_invariants_with_simplify &&
            maybe.ppt.parent.redundant_invs.contains(maybe)) {
          continue;
        }

        // don't print out invariants with min(), max(), or sum() variables
        boolean mms = false;
        VarInfo[] varbls = maybe.ppt.var_infos;
        for (int v=0; !mms && v<varbls.length; v++) {
          mms |= varbls[v].isDerivedSequenceMinMaxSum();
        }
        if (mms) {
          continue;
        }

        if (maybe.ppt.parent.ppt_name.isExitPoint()) {
          for (int i = 0; i < maybe.ppt.var_infos.length; i++) {
            VarInfo vi = maybe.ppt.var_infos[i];
            if (vi.isDerivedParam()) {
              continue;
            }
          }
        }

        Invariant consequent = maybe.consequent();
        Invariant predicate = maybe.predicate();
        Invariant inv, cluster_inv;
        boolean cons_uses_cluster = false, pred_uses_cluster = false;
        // extract the consequent (predicate) if the predicate
        // (consequent) uses the variable "cluster".  Ignore if they
        // both depend on "cluster"
        if (consequent.usesVarDerived("cluster"))
          cons_uses_cluster = true;
        if (predicate.usesVarDerived("cluster"))
          pred_uses_cluster = true;

        if (!(pred_uses_cluster ^ cons_uses_cluster))
          continue;
        else if (pred_uses_cluster) {
          inv = consequent;
          cluster_inv = predicate;
        } else {
          inv = predicate;
          cluster_inv = consequent;
        }

        if (!inv.isInteresting()) {
          continue;
        }

        if (!inv.isWorthPrinting()) {
          continue;
        }

        if (contains_constant_non_012(inv)) {
          continue;
        }

        // filter out unwanted invariants

        // 1) Invariants involving sequences
        if (inv instanceof daikon.inv.binary.twoSequence.TwoSequence ||
            inv instanceof daikon.inv.binary.sequenceScalar.SequenceScalar ||
            inv instanceof daikon.inv.binary.sequenceString.SequenceString ||
            inv instanceof daikon.inv.unary.sequence.SingleSequence ||
            inv instanceof daikon.inv.unary.stringsequence.SingleStringSequence ) {
          continue;
        }

        if (inv instanceof daikon.inv.ternary.threeScalar.LinearTernary ||
            inv instanceof daikon.inv.binary.twoScalar.LinearBinary) {
          continue;
        }

        String inv_string = inv.format_using(OutputFormat.JAVA);
        if (orig_pattern.matcher(inv_string).find()
            || dot_class_pattern.matcher(inv_string).find()) {
          continue;
        }
        String fake_inv_string = simplify_inequalities(inv_string);
        HashedConsequent real = new HashedConsequent(inv, null);
        if (!fake_inv_string.equals(inv_string)) {
          // For instance, inv_string is "x != y", fake_inv_string is "x == y"
          HashedConsequent fake = new HashedConsequent(inv, inv_string);
          boolean added =
            store_invariant(cluster_inv.format_using(OutputFormat.JAVA),
                            fake_inv_string, fake, pptname);
          if (!added) {
            // We couldn't add "x == y", (when we're "x != y") because
            // it already exists; so don't add "x == y" either.
            continue;
          }
        }
        store_invariant(cluster_inv.format_using(OutputFormat.JAVA),
                        inv_string, real, pptname);
      }
    }
  }

  // Store the invariant for later printing. Ignore duplicate
  // invariants at the same program point.
  private static boolean store_invariant (String predicate,
                                          String index,
                                          HashedConsequent consequent,
                                          String pptname) {
    if (!pptname_to_conditions.containsKey(pptname)) {
      pptname_to_conditions.put(pptname, new HashMap<String,Map<String,HashedConsequent>>());
    }

    Map<String,Map<String,HashedConsequent>> cluster_to_conditions = pptname_to_conditions.get(pptname);
    if (!cluster_to_conditions.containsKey(predicate)) {
      cluster_to_conditions.put(predicate, new HashMap<String,HashedConsequent>());
    }

    Map<String,HashedConsequent> conditions = cluster_to_conditions.get(predicate);
    if (conditions.containsKey(index)) {
      HashedConsequent old = conditions.get(index);
      if (old.fakeFor != null && consequent.fakeFor == null) {
        // We already saw (say) "x != y", but we're "x == y", so replace it.
        conditions.remove(index);
        conditions.remove(old.fakeFor);
        conditions.put(index, consequent);
        return true;
      }
      return false;
    } else {
      conditions.put(index, consequent);
      return true;
    }
  }


  private static boolean contains_constant_non_012 (Invariant inv) {
    if (inv instanceof daikon.inv.unary.scalar.OneOfScalar) {
      daikon.inv.unary.scalar.OneOfScalar oneof = (daikon.inv.unary.scalar.OneOfScalar) inv;
      // OneOf invariants that indicate a small set ( > 1 element) of
      // possible values are not interesting, and have already been
      // eliminated by the isInteresting check
      long num = ((Long) oneof.elt()).longValue();
      if (num > 2 || num < -1)
        return true;
    }

    return false;
  }

  // remove non-word characters and everything after ":::" from the
  // program point name, leaving PackageName.ClassName.MethodName
  private static String cleanup_pptname (String pptname) {
    int index;
    if ((index = pptname.indexOf("(")) > 0) {
      pptname = pptname.substring(0, index);
    }

    if (pptname.endsWith("."))
      pptname = pptname.substring(0, pptname.length()-2);

    Matcher m = non_word_pattern.matcher(pptname);
    return m.replaceAll(".");
  }

  /**
   * Prevents the occurence of "equivalent" inequalities, or inequalities
   * which produce the same pair of splits at a program point, for example
   * "x <= y" and "x > y". Replaces ">=" with "<", "<=" with ">", and "!="
   * with "==" so that the occurence of equivalent inequalities can be
   * detected. However it tries not to be smart ... If there is more than
   * one inequality in the expression, it doesn't perform a substitution.
   **/
  private static String simplify_inequalities (String condition) {
    if (contains_exactly_one(condition, inequality_pattern)) {
      if (gteq_pattern.matcher(condition).find())
        condition = gteq_pattern.matcher(condition).replaceFirst("<");
      else if (lteq_pattern.matcher(condition).find())
        condition = lteq_pattern.matcher(condition).replaceFirst(">");
      else if (neq_pattern.matcher(condition).find())
        condition = neq_pattern.matcher(condition).replaceFirst("==");
      else
        throw new Error("this can't happen");
    }
    return condition;
  }

  private static boolean contains_exactly_one (String string,
                                               Pattern pattern) {
    Matcher m = pattern.matcher(string);
    // return true if first call returns true and second returns false
    return (m.find()
            && !m.find());
  }

  static Pattern orig_pattern, dot_class_pattern, non_word_pattern;
  static Pattern gteq_pattern, lteq_pattern, neq_pattern, inequality_pattern;
  static Pattern contradict_inv_pattern, useless_inv_pattern_1, useless_inv_pattern_2;
  static {
    try {
      non_word_pattern = Pattern.compile("\\W+");
      orig_pattern = Pattern.compile("orig\\s*\\(");
      dot_class_pattern = Pattern.compile("\\.class");
      inequality_pattern = Pattern.compile(  "[\\!<>]=");
      gteq_pattern = Pattern.compile(">=");
      lteq_pattern = Pattern.compile("<=");
      neq_pattern = Pattern.compile("\\!=");
      contradict_inv_pattern
        = Pattern.compile("(^| && )(.*) == -?[0-9]+ &.*& \\2 == -?[0-9]+($| && )");
      useless_inv_pattern_1
        = Pattern.compile("(^| && )(.*) > -?[0-9]+ &.*& \\2 > -?[0-9]+($| && )");
      useless_inv_pattern_2
        = Pattern.compile("(^| && )(.*) < -?[0-9]+ &.*& \\2 < -?[0-9]+($| && )");
    } catch (PatternSyntaxException me) {
      throw new Error("ExtractConsequent: Error while compiling pattern" + me);
    }
  }
}
