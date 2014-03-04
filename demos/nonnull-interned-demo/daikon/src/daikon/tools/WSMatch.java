package daikon.tools;

import java.util.*;
import java.util.regex.*;
import java.io.*;

import utilMDE.*;

import checkers.quals.Interned;

import daikon.chicory.DTraceReader;
import daikon.chicory.DeclReader;
import static daikon.chicory.DeclReader.*;

/**
 * Reads multiple dtrace files from web services and looks for fields
 * that match
 */
public class WSMatch {

  /**
   * Used to compare to doubles.  The parameter is how close (ratio) the two
   * doubles must be in order to be considered equal.  Larger ratioes will
   * match more which may have unintended effects on matching columns (since
   * multiple matches in a column result in no match)
   */
  static FuzzyFloat fuzzy = new FuzzyFloat (0.01);

  @Option ("print progress information")
  public static boolean verbose = false;

  @Option ("consider only variables that match the regular expression")
  public static Pattern var_match = null;

  @Option ("minimum rate for a substitution match")
  public static double min_substitution_match = 0.60;

  @Option ("minimum rate for a substitution cross check")
  public static double min_substitution_cross_check = 0.90;

  @Option ("minimum rate for a composable match")
  public static double min_composable_match = 0.60;

  /** Set of variables that are constant **/
  public static Set<DeclVarInfo> constants = new LinkedHashSet<DeclVarInfo>();

  /** Set of variables that are duplicates of an input variable **/
  public static Set<DeclVarInfo> dups = new LinkedHashSet<DeclVarInfo>();

  static SimpleLog debug_substitution = new SimpleLog (false);
  static SimpleLog debug_constants = new SimpleLog (false);
  static SimpleLog debug_dups = new SimpleLog (true);

  /**
   * Information about a match between two operations.  Includes the
   * matching variables, the percentage of matches, and the rows that
   * match
   */
  public static class MatchInfo implements Comparable<MatchInfo> {
    DeclPpt ppt1;
    DeclVarInfo var1;
    DeclPpt ppt2;
    DeclVarInfo var2;
    double perc_match;
    List<RowMatch> matching_rows = new ArrayList<RowMatch>();

    public MatchInfo (DeclPpt ppt1, DeclVarInfo var1, DeclPpt ppt2, DeclVarInfo var2,
                      double perc_match) {

      this.ppt1 = ppt1;
      this.var1 = var1;
      this.ppt2 = ppt2;
      this.var2 = var2;
      this.perc_match = perc_match;
    }

    /** Sort based on percentage of matches **/
    public int compareTo (MatchInfo m1) {
      if (this.perc_match == m1.perc_match)
        return 0;
      else if (this.perc_match < m1.perc_match)
        return -1;
      else
        return 1;
    }

    public String toString() {
      return String.format ("%5.2f  %s.%s  %s.%s", perc_match,
                            ppt1.get_short_name(), var1.name,
                            ppt2.get_short_name(), var2.name);
    }
  }

  /**
   * Pair of DeclVarInfos suitable for a key in a hashmap.  The names of the
   * variables are used as their identifiers
   */
  public static class VarPair {
    DeclVarInfo v1;
    DeclVarInfo v2;
    VarPair (DeclVarInfo v1, DeclVarInfo v2) {
      this.v1 = v1;
      this.v2 = v2;
    }
    public int hashCode() {
      return v1.name.hashCode() * v2.name.hashCode();
    }
    public boolean equals(Object other) {
      if (other instanceof VarPair) {
        VarPair vp = (VarPair) other;
        return (v1.name.equals (vp.v1.name) && v2.name.equals (vp.v2.name));
      }
      return (false);
    }
  }

  /**
   * Information about a operations that are possible substitutions
   * for each other
   */
  public static class Substitution {

    /**
     * List of matching inputs.  All of the inputs much match for an
     * operation to substitute for another
     */
    List<MatchInfo> inputs = new ArrayList<MatchInfo>();

    /** List of matching outputs.  One or more outputs must match **/
    List<MatchInfo> outputs = new ArrayList<MatchInfo>();

    /**
     * Map from each pair of variables of the same type (input or
     * output) to their match info.  Note that this is based on a
     * particular primary match which determines which rows to compare
     * between the two operations.
     */
    Map<VarPair,MatchInfo> vars_match = new LinkedHashMap<VarPair,MatchInfo>();

    /** Input variables for program point 1 from the primary match **/
    List<DeclVarInfo> ppt1_inputs  = new ArrayList<DeclVarInfo>();
    /** Input variables for program point 2 from the primary match **/
    List<DeclVarInfo> ppt2_inputs  = new ArrayList<DeclVarInfo>();
    /** Output variables for program point 1 from the primary match **/
    List<DeclVarInfo> ppt1_outputs = new ArrayList<DeclVarInfo>();
    /** Output variables for program point 2 from the primary match **/
    List<DeclVarInfo> ppt2_outputs = new ArrayList<DeclVarInfo>();

    /** Best matches for input variables **/
    List<MatchInfo> input_matches = new ArrayList<MatchInfo>();

    /** Best matches for output variables **/
    List<MatchInfo> output_matches = new ArrayList<MatchInfo>();

    protected Substitution (MatchInfo primary) {

      // Find the inputs and outputs for ppt 1 from the primary match
      for (DeclVarInfo v : primary.ppt1.get_all_vars()) {
        if (!include_var (v))
          continue;
        if (constants.contains (v))
          continue;
        if (is_input (v))
          ppt1_inputs.add (v);
        else
          ppt1_outputs.add (v);
      }

      // Find the inputs and outputs for ppt 2 from the primary match
      for (DeclVarInfo v : primary.ppt2.get_all_vars()) {
        if (!include_var (v))
          continue;
        if (constants.contains (v))
          continue;
        if (is_input (v))
          ppt2_inputs.add (v);
        else
          ppt2_outputs.add (v);
      }

      // Build the var match information for each combination of variables
      // input-input and output-output
      for (DeclVarInfo var1 : ppt1_inputs) {
        for (DeclVarInfo var2 : ppt2_inputs) {
          MatchInfo mi = compare_var (primary, primary.ppt1, var1,
                                      primary.ppt2, var2);
          vars_match.put (new VarPair (var1, var2), mi);
        }
      }
      for (DeclVarInfo var1 : ppt1_outputs) {
        for (DeclVarInfo var2 : ppt2_outputs) {
          MatchInfo mi = compare_var (primary, primary.ppt1, var1,
                                      primary.ppt2, var2);
          vars_match.put (new VarPair (var1, var2), mi);
        }
      }
      debug_substitution.log ("%s inputs: %s\n", primary.ppt1.get_short_name(),
                              ppt1_inputs);
      debug_substitution.log ("%s outputs: %s\n",
                              primary.ppt1.get_short_name(), ppt1_outputs);
      debug_substitution.log ("%s inputs: %s\n", primary.ppt2.get_short_name(),
                              ppt2_inputs);
      debug_substitution.log ("%s outputs: %s\n",
                              primary.ppt2.get_short_name(), ppt2_outputs);


      // Find the best input matches
      if (ppt1_inputs.size() == ppt2_inputs.size())
        input_matches = find_best_matches (ppt1_inputs, ppt2_inputs,
                                           min_substitution_cross_check);

      // Find the best output matches
      output_matches = find_best_matches (ppt1_outputs, ppt2_outputs,
                                          min_substitution_cross_check);
    }

    /**
     * Returns whether or not this is a valid substitute.  To be valid
     * each of the inputs from the first operation must match a distinct
     * input in the second operation and at least one output must match
     */
    public boolean is_valid() {
      if (input_matches.size() != ppt1_inputs.size())
        return false;

      if (output_matches.size() == 0)
        return false;

      return true;
    }
    /**
     * Finds the best set of matches between the variables in vars1 and
     * those in vars2.  The best set of matches is the set that has the
     * the most elements whose match percentage is greater than min_percent.
     * If multiple sets have the same number of elements, the set with the
     * higher average match percent is chosen.
     */
    public List<MatchInfo> find_best_matches (List<DeclVarInfo> vars1,
                                              List<DeclVarInfo> vars2,
                                              double min_percent) {

      debug_substitution.log ("Looking for matches between %s and %s at %f%n",
                              vars1, vars2, min_percent);
      List<MatchInfo> best_match = new ArrayList<MatchInfo>();
      matches (vars1, vars2, 0, min_percent, new ArrayList<MatchInfo>(),
               best_match);
      debug_substitution.log ("Found matches: %s%n", best_match);
      return best_match;
    }

    /**
     * Recursively explore all of the possible combinations of matches
     * between the variables in vars1 and vars2.
     *
     * @param vars1 List of variables from the first operation
     * @param vars2 List of variables from the second operation
     * @param index Index of current variable in vars1
     * @param min_perc The minimum percentage of matches required to call
     *    a variable pair a match
     * @param matches List good matches for the variables in vars1 that
     *    have already been processed.
     * @param best_match The best match found so far.  Updated in place
     *    when a better match is found.
     */
    private void matches (List<DeclVarInfo> vars1, List<DeclVarInfo> vars2,
                         int index, double min_perc,
                         List<MatchInfo> matches,
                         List<MatchInfo> best_match) {

      // If there are no more variables to consider, replace best_match
      // with matches iff it is a better match.
      if ((index >= vars1.size()) || (vars2.size() == 0)) {
        if (better_match (best_match, matches)) {
          best_match.clear();
          best_match.addAll (matches);
        }
        return;
      }

      // Loop through each variable in vars2 and determine its match with
      // the current v1.   If it is better than the minimum percentage,
      // add it to the list of matches and remove it from the vars2 to
      // consider for other matches.  In either case explore the
      // remaining variables in vars1.  Note that we only need to
      // explore the other variables once if we don't have a match (since
      // the result will be the same)
      DeclVarInfo v1 = vars1.get (index);
      // System.out.printf ("Processing variable %s [%d/%d]%n", v1, index,
      //                   vars1.size());
      boolean no_match = false;
      for (int ii = 0; ii < vars2.size(); ii++) {
        DeclVarInfo v2 = vars2.get(ii);
        MatchInfo m = vars_match.get (new VarPair(v1, v2));
        if (m.perc_match > min_perc) {
          // System.out.printf ("Adding match %s%n", m);
          matches.add (m);
          List<DeclVarInfo> vars2_remaining = new ArrayList<DeclVarInfo>(vars2);
          vars2_remaining.remove (ii);
          matches (vars1, vars2_remaining, index+1, min_perc, matches,
                   best_match);
          matches.remove (matches.size() - 1);
        } else if (!no_match) {
          no_match = true;
          matches (vars1, vars2, index+1, min_perc, matches, best_match);
        }
      }
    }

    /**
     * Returns true if m2 is a better match than m1, false otherwise.
     * A match is better if it contains more matches.  If each has the
     * the same number of matches, the one with the highest average match
     * percentage is better.
     */
    private boolean better_match (List<MatchInfo> m1, List<MatchInfo> m2) {

      if (m2.size() > m1.size())
        return (true);

      if (m2.size() == m1.size()) {
        double m1_total = 0.0;
        double m2_total = 0.0;
        for (MatchInfo m : m1)
          m1_total += m.perc_match;
        for (MatchInfo m : m2)
          m2_total += m.perc_match;
        if (m2_total > m1_total)
          return (true);
      }

      return (false);
    }

    /**
     * Look for a substitution given a primary match.  A valid
     * substitution requires that all input parameters match and that
     * one or more output parameters match.  Returns the substitution if
     * it is valid, otherwise returns null
     */
    public static Substitution check_substitution (MatchInfo primary) {

      Substitution s = new Substitution (primary);
      if (s.is_valid())
        return (s);
      else
        return (null);
    }
  }

  /** Tracks the sample numbers in two matching samples **/
  public static class RowMatch {
    int index1;
    int index2;
    RowMatch (int index1, int index2) {
      this.index1 = index1; this.index2 = index2;
    }
    public String toString () {
      return (index1 + "-" + index2);
    }
  }

  public static void main (String[] args) {

    Options options = new Options ("WSMatch [options] dtrace-files...",
                                   WSMatch.class);
    String[] files = options.parse_and_usage (args);

    // Read in all of the files.  Change all ppt names to include the
    // filenames since some operation names are the same
    List<DTraceReader> traces = new ArrayList<DTraceReader>();
    for (String file : files) {
      if (verbose)
        System.out.printf ("Processing file %s%n", file);
      DTraceReader trace = new DTraceReader();
      File tracefile = new File (file);
      trace.read (tracefile);
      for (DeclPpt ppt : trace.get_all_ppts())
        ppt.name = tracefile.getName().replaceFirst ("[.].*", "") + "."
          + ppt.name;
      traces.add (trace);
    }

    // Find all of the constants
    for (DTraceReader trace : traces) {
      for (DeclPpt ppt : trace.get_all_ppts())
        constants.addAll (find_constants (ppt));
    }

    // Find all of the duplicate
    for (DTraceReader trace : traces) {
      for (DeclPpt ppt : trace.get_all_ppts())
        dups.addAll (find_dups (ppt));
    }

    print_input_stats (traces);

    List<MatchInfo> substitute_matches = new ArrayList<MatchInfo>();
    List<MatchInfo> compose_matches = new ArrayList<MatchInfo>();

    // Look for any matches between each pair of columns
    for (int ii = 0; ii < traces.size(); ii++) {
      for (int jj = ii+1; jj < traces.size(); jj++) {
        List<MatchInfo> results
          = compare_services (traces.get(ii), traces.get(jj));
        if (results.size() > 0) {
          List<MatchInfo> substitute_results = new ArrayList<MatchInfo>();
          List<MatchInfo> compose_results = new ArrayList<MatchInfo>();
          split_results (results, substitute_results, compose_results);
          if (substitute_results.size() > 0)
            substitute_matches.add (Collections.max (substitute_results));
          for (MatchInfo m : compose_results) {
            if (m.perc_match >= min_composable_match)
              compose_matches.add (m);
          }
        }
      }
    }

    // For each substitution match, look for corresponding matches in the
    // other fields
    for (MatchInfo primary_match : substitute_matches) {
      if (primary_match.perc_match < min_substitution_match)
        continue;
      List<MatchInfo> subs = find_substitutes (primary_match);
      System.out.printf ("%nSubstitution matches for primary %s%n",
                         primary_match);
      if (verbose)
        System.out.printf ("  matching rows = %s%n",
                           primary_match.matching_rows);
      for (MatchInfo sub : subs) {
        if (sub.perc_match < min_substitution_cross_check)
          continue;
        System.out.printf ("  %s%n", sub);
      }
    }

    // More precise substitution matching
    for (MatchInfo primary_match : substitute_matches) {
      if (primary_match.perc_match < min_substitution_match)
        continue;
      System.out.printf ("%nChecking Substitution matches for primary %s%n",
                         primary_match);
      Substitution sub = Substitution.check_substitution (primary_match);
      if (sub != null) {
        System.out.printf ("  Input matches:%n");
        for (MatchInfo m : sub.input_matches)
          System.out.printf ("    %s%n", m);
        System.out.printf ("  Output matches:%n");
        for (MatchInfo m : sub.output_matches)
          System.out.printf ("    %s%n", m);
      }
    }

    System.out.printf ("%nComposition Matches:%n");
    for (MatchInfo m : compose_matches) {
      if (m.perc_match < min_composable_match)
        continue;
      System.out.printf ("%5.2f %s.%s  %s.%s%n", m.perc_match,
                         m.ppt1, m.var1.name, m.ppt2, m.var2.name);
      if (verbose)
        System.out.printf ("  matching rows = %s%n", m.matching_rows);
    }
  }

  /**
   * Compares each field from two services and returns how often they
   * match up
   */
  public static List<MatchInfo> compare_services (DTraceReader trace1,
                                                  DTraceReader trace2) {

    List<MatchInfo> results = new ArrayList<MatchInfo>();

    DeclPpt ppt1 = trace1.get_all_ppts().get(0);
    DeclPpt ppt2 = trace2.get_all_ppts().get(0);

    for (DeclVarInfo var1 : ppt1.get_all_vars()) {
      if (!include_var (var1))
        continue;
      for (DeclVarInfo var2 : ppt2.get_all_vars()) {
        if (!include_var (var2))
          continue;
        results.add (compare_var (ppt1, var1,  ppt2, var2));
      }
    }
    return (results);
  }

  /**
   * Compares all of the values on the specified variables.  Returns the
   * percentage of rows that match exactly once
   */
  public static MatchInfo compare_var (DeclPpt ppt1, DeclVarInfo var1,
                                       DeclPpt ppt2, DeclVarInfo var2) {

    //System.out.printf ("%s index = %d, %s index = %d\n", var1, var1.index,
    //                   var2, var2.index);

    @SuppressWarnings("interned") // checker bug
    List<List</*@Interned*/ Object>> data1 = ppt1.get_var_data();
    @SuppressWarnings("interned") // checker bug
    List<List</*@Interned*/ Object>> data2 = ppt2.get_var_data();

    MatchInfo m = new MatchInfo (ppt1, var1, ppt2, var2, 0.0);

    int possible_matches = Math.min (data1.size(), data2.size());
    int match_cnt = 0;
    for (int ii = 0; ii < data1.size(); ii++) {
      List</*@Interned*/ Object> var_data1 = data1.get(ii);
      int mcnt1 = 0;
      int row = -1;
      for (int jj = 0; jj < data2.size(); jj++) {
        List</*@Interned*/ Object> var_data2 = data2.get(jj);
        boolean match = compare_val (var1, var_data1.get(var1.index),
                                     var2, var_data2.get(var2.index));
        if (match) {
          mcnt1++;
          row = jj;
        }
      }
      if (mcnt1 == 1) {
        match_cnt++;
        m.matching_rows.add (new RowMatch (ii, row));
      } else if (mcnt1 > 1) {
        // System.out.printf ("var1 %s value %s matches %s %d times%n", var1,
        //                   var_data1.get(var1.index), var2, mcnt1);
      }
    }

    m.perc_match = ((double) match_cnt) / possible_matches;
    if ((m.perc_match > 0) && (verbose))
      System.out.printf ("%5.2f  %s.%s %s.%s%n", m.perc_match, ppt1.name,
                         var1.name, ppt2.name, var2.name);
    return (m);
  }

  /**
   * Returns whether or not the two values are at least approximately
   * the same.  Nonsensical values are always different.
   */
  public static boolean compare_val (DeclVarInfo var1, /*@Interned*/ Object data1,
                                     DeclVarInfo var2, /*@Interned*/ Object data2) {

    // System.out.printf ("Comparing %s = %s against %s = %s\n", var1, data1,
    //                   var2, data2);

    // Nonsensical values are never equal
    if ((data1 == null) || (data2 == null))
      return (false);

    if (var1.is_int() && var2.is_int()) {
      int i1 = (Integer) data1;
      int i2 = (Integer) data2;
      return (i1 == i2);
    } else if (var1.is_double() && var2.is_double()) {
      double d1 = (Double) data1;
      double d2 = (Double) data2;
      return fuzzy.eq (d1, d2);
    } else if (var1.is_string() && var2.is_string()) {
      String s1 = (String) data1;
      String s2 = (String) data2;
      return s1.equalsIgnoreCase (s2);
    } else if (var1.is_string() && var2.is_double()) {
      double d1;
      try {
        d1 = Double.parseDouble ((String) data1);
      } catch (Throwable t) {
        return (false);
      }
      double d2 = (Double) data2;
      return fuzzy.eq (d1, d2);
    } else if (var2.is_string() && var1.is_double()) {
      double d2;
      try {
        d2 = Double.parseDouble ((String) data2);
      } catch (Throwable t) {
        return (false);
      }
      double d1 = (Double) data1;
      return fuzzy.eq (d1, d2);
    } else { // non-matching types
      return (false);
    }
  }

  /**
   * Finds substitutes given a match.  Only compare input to input and
   * output to output.
   */
  public static List<MatchInfo> find_substitutes (MatchInfo match) {

    List<MatchInfo> results = new ArrayList<MatchInfo>();

    for (DeclVarInfo var1 : match.ppt1.get_all_vars()) {
      if (!include_var (var1))
        continue;
      for (DeclVarInfo var2 : match.ppt2.get_all_vars()) {
        if (!include_var (var2))
          continue;
        if (is_input(var1) != is_input(var2))
          continue;
        results.add (compare_var (match, match.ppt1, var1,  match.ppt2, var2));
      }
    }
    return (results);
  }

  /**
   * Compares the values using the matching rows specified in match.
   * Returns a MatchInfo with perc_match filled in accordingly.  Does not
   * fill in matching_rows.
   */
  public static MatchInfo compare_var (MatchInfo match, DeclPpt ppt1,
                                  DeclVarInfo var1, DeclPpt ppt2, DeclVarInfo var2) {

    @SuppressWarnings("interned") // checker bug
    List<List</*@Interned*/ Object>> data1 = ppt1.get_var_data();
    @SuppressWarnings("interned") // checker bug
    List<List</*@Interned*/ Object>> data2 = ppt2.get_var_data();

    MatchInfo result = new MatchInfo (ppt1, var1, ppt2, var2, 0.0);

    int match_cnt = 0;
    for (RowMatch row : match.matching_rows) {
      List</*@Interned*/ Object> var_data1 = data1.get(row.index1);
      List</*@Interned*/ Object> var_data2 = data2.get(row.index2);
      boolean val_match = compare_val (var1, var_data1.get(var1.index),
                                       var2, var_data2.get(var2.index));
      if (val_match) {
        match_cnt++;
      }
    }

    int possible_matches = match.matching_rows.size();
    result.perc_match = ((double) match_cnt) / possible_matches;
    return (result);
  }

  /**
   * Finds all of the constants variables in the ppt.  A constant variable
   * is one that has the same value for each data sample.  Null values are
   * ignored (as they are nonsensical)
   */
  public static List<DeclVarInfo> find_constants (DeclPpt ppt) {

    List<DeclVarInfo> constants = new ArrayList<DeclVarInfo>();
    @SuppressWarnings("interned") // checker bug
    List<List</*@Interned*/ Object>> data = ppt.get_var_data();

    // Loop through each variable
    for (DeclVarInfo v : ppt.get_all_vars()) {

      boolean constant = true;
      boolean always_missing = true;
      Object first_val = null;

      // Loop through each remaining sample, exit if a sample is not equal
      for (List<Object> sample : data) {
        Object val = sample.get(v.index);
        if (val == null)
          continue;
        always_missing = false;
        if (first_val == null)
          first_val = val;
        else if (!first_val.equals(val)) {
          constant = false;
          break;
        }
      }

      if (constant && !always_missing) {
        constants.add (v);
        debug_constants.log ("Variable %s in ppt %s is constant%n", v.name,
                             ppt);
      }
    }

    return (constants);
  }

  /**
   * Looks for output variables that are duplicates of an input variable.
   * Null (nonsensical) values are ignored.
   */
  public static List<DeclVarInfo> find_dups (DeclPpt ppt) {

    List<DeclVarInfo> dups = new ArrayList<DeclVarInfo>();
    @SuppressWarnings("interned") // checker bug
    List<List</*@Interned*/ Object>> data = ppt.get_var_data();

    // Find the input and output variables
    List<DeclVarInfo> inputs = new ArrayList<DeclVarInfo>();
    List<DeclVarInfo> outputs = new ArrayList<DeclVarInfo>();
    for (DeclVarInfo v : ppt.get_all_vars()) {
      if (is_input (v))
        inputs.add (v);
      else
        outputs.add (v);
      }

    // Compare each input against each output.  If every available value
    // matches, note the output as a duplicate
    for (DeclVarInfo input : inputs) {
      for (DeclVarInfo output : outputs) {
        boolean duplicate = true;
        boolean always_missing = true;
        for (List</*@Interned*/ Object> samples : data) {
          /*@Interned*/ Object input_val = samples.get (input.index);
          /*@Interned*/ Object output_val = samples.get (output.index);
          if (output_val != null)
            always_missing = false;
          if ((input_val == null) || (output_val == null))
            continue;
          if (!compare_val (input, input_val, output, output_val)) {
            duplicate = false;
            break;
          }
        }
        if (duplicate && !always_missing) {
          dups.add (output);
          debug_dups.log ("Added duplicate variable %s%n", output);
        }
      }
    }

    return (dups);
  }


  /**
   * Prints the results of comparing two services
   */
  public static void print_results (List<MatchInfo> results) {

    if (results.size() == 0)
      return;

    Collections.sort (results, Collections.reverseOrder());
    MatchInfo m = results.get(0);
    System.out.printf ("%5.2f %s.%s  %s.%s%n", m.perc_match,
                       m.ppt1, m.var1.name, m.ppt2, m.var2.name);
  }

  /**
   * Takes all of the matches and splits them into those between the
   * same type of parameters (in-in/out-out (substitute_results)) and
   * those between different types of parements (in-out/out-in
   * (compose_results)).
   */
  public static void split_results (List<MatchInfo> all_results,
        List<MatchInfo> substitute_results, List<MatchInfo> compose_results) {

    for (MatchInfo m : all_results) {
      if (is_input (m.var1) == is_input (m.var2))
        substitute_results.add (m);
      else
        compose_results.add (m);
    }
  }

  public static boolean is_input (DeclVarInfo v) {
    return v.name.startsWith ("input");
  }

  /**
   * Returns whether or not to consider this variable.  Variables that
   * are duplicates or that are specified by the user to be ignored are
   * not included
   **/
  public static boolean include_var (DeclVarInfo var) {

    if (dups.contains (var))
      return (false);

    if (var_match == null)
      return true;

    Matcher m = var_match.matcher (var.name);
    return (m.find());
  }

  public static void print_input_stats (List<DTraceReader> traces) {

    int op_cnt = 0;
    int total_outputs = 0;
    int input_constants = 0;
    int output_constants = 0;
    int total_dups = 0;

    for (DTraceReader trace : traces) {
      for (DeclPpt ppt : trace.get_all_ppts()) {
        op_cnt++;
        List<DeclVarInfo> inputs = new ArrayList<DeclVarInfo>();
        List<DeclVarInfo> outputs = new ArrayList<DeclVarInfo>();
        for (DeclVarInfo v : ppt.get_all_vars()) {
          if (constants.contains (v)) {
            if (is_input(v)) input_constants++;
            else output_constants++;
          }
          else if (dups.contains (v))
            total_dups++;
          if (is_input (v))
            inputs.add (v);
          else
            outputs.add(v);
        }
        System.out.printf ("%noperation %s (%d inputs, %d outputs)%n", ppt,
                           inputs.size(), outputs.size());
        System.out.printf (" inputs:%n");
        print_vars ("    ", inputs);
        System.out.printf (" outputs:%n");
        print_vars ("    ", outputs);
        total_outputs += outputs.size();
      }
    }

    System.out.printf ("%nTotal operations       = %d%n", op_cnt);
    System.out.printf   ("Total outputs          = %d%n", total_outputs);
    System.out.printf   ("Total input constants  = %d%n", input_constants);
    System.out.printf   ("Total output constants = %d%n", output_constants);
    System.out.printf   ("Total dups             = %d%n", total_dups);
  }

  /** Prints each variable on a separate line **/
  public static void print_vars (String prefix, List<DeclVarInfo> vars) {

    for (DeclVarInfo v : vars) {
      String constant_str = "";
      if (constants.contains (v))
        constant_str = " [constant]";
      String dup_str = "";
      if (dups.contains (v))
        dup_str = " [duplicate]";
      System.out.printf ("%s%-8s %s%s%s%n", prefix, v.type, v.get_name(),
                         constant_str, dup_str);
    }
  }

}
