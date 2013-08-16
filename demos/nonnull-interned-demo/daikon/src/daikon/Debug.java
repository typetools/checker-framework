package daikon;

import daikon.inv.*;
import java.io.*;
import java.util.*;

import java.util.logging.Logger;
import java.util.logging.Level;

import utilMDE.*;

/**
 * Debug class used with the logger to create standardized output.
 * It can be setup to track combinations of classes, program points,
 * and variables.  The most common class to track is an invariant, but
 * any class can be used.
 *
 * This allows detailed information about a particular class/ppt/variable
 * combination to be printed without getting lost in a mass of other
 * information (which is a particular problem in Daikon due to the volume
 * of data considered).
 *
 * Note that each of the three items (class, ppt, variable) must match
 * in order for a print to occur.
 **/

public class Debug {

  /** Debug Logger. */
  public static final Logger debugTrack = Logger.getLogger ("daikon.Debug");

  /**
   * List of classes for logging. Each name listed is compared to the
   * fully qualified class name.  If it matches (shows up anywhere in
   * the class name) it will be included in debug prints.  This is
   * not a regular expression match
   *
   * @see #log(Logger, Class, Ppt, String)
   */

  public static String[] debugTrackClass
    = {
      // "Bound",
      // "DynamicConstants",
      // "EltNonZero",
      // "EltNonZeroFloat",
      // "EltOneOf",
      // "Equality",
      // "FunctionBinary",
      // "IntEqual",
      // "IntGreaterEqual",
      // "IntGreaterThan",
      // "IntLessEqual",
      // "IntLessThan",
      // "IntNonEqual",
      // "LinearBinary",
      // "LowerBound",
      // "Member",
      // "NonZero",
      // "OneOfSequence",
      // "PptSlice",
      // "PptSlice2",
      // "PptSliceEquality",
      // "PptTopLevel",
      // "SeqIndexComparison",
      // "SeqIndexNonEqual",
      // "SeqIntEqual",
      // "SeqSeqIntEqual",
      // "NonZero",
      // "FunctionBinary",
      // "OneOfSequence",
      // "IntLessEqual",
      // "IntGreaterEqual",
      // "IntLessThan",
      // "IntGreaterThan",
      // "IntNonEqual",
      // "Member",
      // "FunctionBinary"
      // "EltNonZero",
      // "SeqSeqIntGreaterThan",
      // "SeqSeqIntLessThan",
      // "SubSet",
      // "SuperSet",
      // "EltOneOf",
      // "Bound",
      // "SeqSeqIntLessThan",
      // "SeqSeqIntGreaterThan",
      // "OneOf"
      // "StringComparison",
      // "StringLessThan",
      // "StringGreaterThan",
      // "Modlong_zxy",
      // "UpperBound",
   };

  /**
   * Restrict function binary prints to the specified method.  Implementation
   * is in the FunctionBinary specific log functions.  If null, there is no
   * restriction (all function binary methods are printed).  See Functions.java
   * for a list of function names
   */
  public static String function_binary_method =
     null
    // "java.lang.Math.max("
    // "java.lang.Math.min("
    // "utilMDE.MathMDE.logicalXor("
    // "utilMDE.MathMDE.gcd("
    ;

  /**
   * List of Ppts for logging. Each name listed is compared to
   * the full program point name. If it matches (shows up anywhere in
   * the ppt name) it will be included in the debug prints.  This is
   * not a regular expression match
   *
   * @see #log(Logger, Class, Ppt, String)
   */

  public static String[] debugTrackPpt
    = {
      // "DataStructures.DisjSets.unionDisjoint(int, int):::EXIT",
      // "DataStructures.StackAr.StackAr(int):::EXIT",
      // "DataStructures.StackAr.makeEmpty():::EXIT",
      // "DataStructures.StackAr.makeEmpty()V:::ENTER",
      // "DataStructures.StackAr.top():::EXIT74",
      // "GLOBAL",
      // "std.main(int;char **;)int:::EXIT",
    };

  /**
   * List of variable names for logging. Each name listed is compared
   * to each variable in turn.  If each matches exactly it will be
   * included in track debug prints.  This is not a regular expression
   * match.  Note that the number of variables must match the slice
   * exactly.
   *
   * @see #log(Logger, Class, Ppt, String)
   */

  public static String[][] debugTrackVars
    = {
      // {"::printstats"},
      // {"misc.Fib.STEPS", "orig(misc.Fib.a)"},
      // {"misc.Fib.a", "misc.Fib.STEPS"},
      // {"return"},
    };

  // cached standard parts of the debug print so that multiple calls from
  // the same context don't have to repeat these each time

  /** True if the cached variables are printable. **/
  public boolean cache_match = true;

  /** cached class */
  public Class cache_class;

  /** cached ppt */
  public Ppt cache_ppt;

  /** cached variables */
  public VarInfo[] cache_vis;

  /**
   * Sets the cache for class, ppt, and vis so that future calls to log
   * don't have to set them.
   **/

  public Debug (Class c, Ppt ppt, VarInfo[] vis) {
    set (c, ppt, vis);
  }

  /**
   * Returns a Debug object if the specified class, ppt, and vis match
   * what is being tracked.  Otherwise, return NULL.  Preferred over calling
   * the constructor directly, since it doesn't create the object if it
   * doesn't have to
   */
  public static Debug newDebug (Class c, Ppt ppt, VarInfo[] vis) {
    if (logOn() && class_match (c) && ppt_match (ppt) && var_match (vis))
      return new Debug (c, ppt, vis);
    else
      return null;
  }


  /**
   * Sets up the cache for c, ppt, and whatever variable (if any) from
   * vis that is on the debugTrackVar list.  Essentially this creates
   * a debug object that will print if any of the variables in vis are
   * being tracked (and c and ppt match)
   */
  public Debug (Class c, Ppt ppt, List<VarInfo> vis) {

    VarInfo v = visTracked (vis);
    if (v != null) {
      set (c, ppt, new VarInfo[] {v});
    } else if (vis.size() > 0) {
      set (c, ppt, new VarInfo[] {vis.get(0)});
    } else {
      set (c, ppt, null);
    }
  }

  /**
   * Looks for each of the variables in vis in the DebugTrackVar list.  If
   * any match, returns that variable.  Null is returned if there are no
   * matches.
   */
  public VarInfo visTracked (List<VarInfo> vis) {

    for (VarInfo v : vis) {
      Set<VarInfo> evars = null;
      if (v.equalitySet != null)
        evars = v.equalitySet.getVars();
      if (evars != null) {
        for (VarInfo ev : evars) {
          for (int k = 0; k < debugTrackVars.length; k++) {
            if (ev.name().equals (debugTrackVars[k][0]))
              return (v);
          }
        }
      }
    }

    return null;
  }

  private static String[] ourvars = new String[3];

  private static final VarInfo[] vis1 = new VarInfo[1];
  private static final VarInfo[] vis2 = new VarInfo[2];
  private static final VarInfo[] vis3 = new VarInfo[3];

  public static VarInfo[] vis (VarInfo v1) {
    vis1[0] = v1;
    return (vis1);
  }

  public static VarInfo[] vis (VarInfo v1, VarInfo v2) {
    vis2[0] = v1;
    vis2[1] = v2;
    return (vis2);
  }

  public static VarInfo[] vis (VarInfo v1, VarInfo v2, VarInfo v3) {
    vis3[0] = v1;
    vis3[1] = v2;
    vis3[2] = v3;
    return (vis3);
  }

  /**
   * Sets the cache for class, ppt, and vis so that future calls to log
   * don't have to set them.
   **/

  void set (Class c, Ppt ppt, VarInfo[] vis) {
    cache_class = c;
    cache_ppt = ppt;
    cache_vis = vis;
    if (c == null)
      System.out.println ("Class = null");
    if (ppt == null)
      System.out.println ("ppt = null");
    if (vis == null)
      System.out.println ("vis = null");
    else {
      for (int i = 0; i < vis.length; i++) {
        if (vis[i] == null)
          System.out.println ("vis[" + i + "] == null");
      }
    }
    cache_match = class_match (c) && ppt_match (ppt) && var_match (vis);
  }


  /**
   * Determines whether or not traceback information is printed for each
   * call to log.
   *
   * @see #log(Logger, Class, Ppt, String)
   */
  public static boolean dkconfig_showTraceback = false;

  /**
   * Determines whether or not detailed info (such as from
   * <code>add_modified</code>) is printed.
   *
   * @see #log(Logger, Class, Ppt, String)
   * @see #logDetail()
   */
  public static boolean dkconfig_logDetail = false;

  /**
   * Returns whether or not detailed logging is on.  Note that this check
   * is not performed inside the logging calls themselves, it must be
   * performed by the caller.
   *
   * @see #log(Logger, Class, Ppt, String)
   * @see #logOn()
   */

  public static final boolean logDetail () {
    return (dkconfig_logDetail && debugTrack.isLoggable(Level.FINE));
  }

  /**
   * Returns whether or not logging is on.
   *
   * @see #log(Logger, Class, Ppt, String)
   */

  public static final boolean logOn() {
    return debugTrack.isLoggable(Level.FINE);
  }

  /**
   * Logs the cached class, cached ppt, cached variables and the
   * specified msg via the logger as described in {@link
   * #log(Logger, Class, Ppt, VarInfo[], String)}.
   */

  public void log (Logger debug, String msg) {
    if (cache_match)
      log (debug, cache_class, cache_ppt, cache_vis, msg);
  }

  /**
   * Logs a description of the class, ppt, ppt variables and the
   * specified msg via the logger as described in {@link
   * #log(Logger, Class, Ppt, VarInfo[], String)}.
   */

  public static void log (Logger debug, Class inv_class, Ppt ppt, String msg) {
    if (ppt == null)
      log (debug, inv_class, ppt, null, msg);
    else
      log (debug, inv_class, ppt, ppt.var_infos, msg);
  }

  /**
   * Logs a description of the class, ppt, variables and the specified
   * msg via the logger.  The class, ppt, and variables are
   * checked against those described in {@link #debugTrackClass},
   * {@link #debugTrackPpt}, and {@link #debugTrackVars}.  Only
   * those that match are printed.  Variables will match if they are
   * in the same equality set.  The information is written as: <p>
   *
   * <code> class: ppt : var1 : var2 : var3 : msg </code> <p>
   *
   * Note that if {@link #debugTrack} is not enabled then
   * nothing is printed.  It is somewhat faster to check {@link #logOn()}
   * directly rather than relying on the check here. <p>
   *
   * Other versions of this method (noted below) work without the Logger
   * parameter and take class, ppt, and vis from the cached values
   *
   * @param debug       A second Logger to query if debug tracking is turned
   *                    off or does not match.  If this logger is
   *                    enabled, the same information will be written
   *                    to it.  Note that the information is never
   *                    written to both loggers.
   * @param inv_class   The class.  Can be obtained in a static context
   *                    by ClassName.class
   * @param ppt         Program point
   * @param vis         Variables at the program point.  These are sometimes
   *                    different from the ones in the ppt itself.
   * @param msg         String message to log
   *
   * @see #logOn()
   * @see #logDetail()
   * @see #log(Class, Ppt, VarInfo[], String)
   * @see #log(Class, Ppt, String)
   * @see #log(Logger, String)
   * @see #log(String)
   */

  public static void log (Logger debug, Class inv_class, Ppt ppt,
                          VarInfo[] vis, String msg) {

    // Try to log via the logger first
    if (log (inv_class, ppt, vis, msg))
      return;

    // If debug isn't turned on, there is nothing to do
    if (!debug.isLoggable(Level.FINE))
      return;

    // Get the non-qualified class name
    String class_str = "null";
    if (inv_class != null)
      class_str = UtilMDE.replaceString (inv_class.getName(),
                                inv_class.getPackage().getName() + ".", "");

    // Get a string with all of the variable names.  Each is separated by ': '
    // 3 variable slots are always setup for consistency
    String vars = "";
    for (VarInfo v: vis) {
      vars += v.name() + ": ";
    }
    for (int i = vis.length; i < 3; i++) {
      vars += ": ";
    }

    // Figure out the sample count if possible
    String samp_str = "";
    if (ppt instanceof PptSlice) {
      PptSlice pslice = (PptSlice) ppt;
      samp_str = " s" + pslice.num_samples();
    }

    // Figure out the line number if possible
    String line = " line=?";
    if ((FileIO.data_trace_state != null)
        && (FileIO.data_trace_state.reader != null)) {
      LineNumberReader lnr = FileIO.data_trace_state.reader;
      line = " line=" + String.valueOf(lnr.getLineNumber());
    }

    debug.fine (class_str + ": " + ppt.name()
                 + samp_str + line + ": " + vars + msg);
    if (dkconfig_showTraceback) {
      Throwable stack = new Throwable("debug traceback");
      stack.fillInStackTrace();
      stack.printStackTrace();
    }
  }

 /**
  * Logs a description of the cached class, ppt, and variables and the
  * specified msg via the logger as described in {@link
  * #log(Logger, Class, Ppt, VarInfo[], String)}
  *
  * @return whether or not it logged anything
  */

  public boolean log (String msg) {
    if (!logOn())
      return (false);
    return (log (cache_class, cache_ppt, cache_vis, msg));
  }

  /**
   * Logs a description of the class, ppt, ppt variables and the
   * specified msg via the logger as described in {@link
   * #log(Logger, Class, Ppt, VarInfo[], String)}.
   *
   * @return whether or not it logged anything
   */
  public static boolean log (Class inv_class, Ppt ppt, String msg) {

    return (log (inv_class, ppt, ppt.var_infos, msg));
  }

  /**
   * Logs a description of the class, ppt, variables and the specified
   * msg via the logger as described in {@link #log(Logger,
   * Class, Ppt, String)}.  Accepts vis because sometimes the
   * variables are different from those in the ppt.
   *
   * @return whether or not it logged anything
   */
  public static boolean log (Class inv_class, Ppt ppt, VarInfo[] vis,
                             String msg) {

    if (!debugTrack.isLoggable(Level.FINE))
      return (false);

    // Make sure the class matches
    if (!class_match (inv_class))
      return (false);

    // Make sure the Ppt matches
    if (!ppt_match (ppt))
      return (false);

    // Make sure the variables match
    if (!var_match (vis))
      return (false);

    // Get the non-qualified class name
    String class_str = "null";
    if (inv_class != null)
      class_str = UtilMDE.replaceString (inv_class.getName(),
                                inv_class.getPackage().getName() + ".", "");

    // Get a string with all of the variable names.  Each is separated by ': '
    // 3 variable slots are always setup for consistency
    String vars = "";
    int numvars = (vis == null) ? 0 : vis.length;
    for (int i = 0; i < numvars; i++) {
      VarInfo v = vis[i];
      vars += v.name();
      if (ourvars[i] != null)
        vars += " {" + ourvars[i] + "}";
      vars += ": ";
    }
    for (int i = numvars; i < 3; i++)
      vars += ": ";

    // Figure out the sample count if possible
    String samp_str = "";
    if (ppt instanceof PptSlice) {
      PptSlice pslice = (PptSlice) ppt;
      samp_str = " s" + pslice.num_samples();
    }

    // Figure out the line number if possible
    LineNumberReader lnr = null;
    if (FileIO.data_trace_state != null)
      lnr = FileIO.data_trace_state.reader;
    String line = (lnr == null) ? "?" : String.valueOf(lnr.getLineNumber());
    line = " line=" + line;

    debugTrack.fine (class_str + ": " + ((ppt == null) ? "null" : ppt.name())
                     + samp_str + line + ": " + vars + msg);
    if (dkconfig_showTraceback) {
      Throwable stack = new Throwable("debug traceback");
      stack.fillInStackTrace();
      stack.printStackTrace();
    }

    return (true);
  }

  /**
   * Returns whether or not the specified class matches the classes being
   * tracked
   */
  public static boolean class_match (Class inv_class) {

    if ((debugTrackClass.length > 0) && (inv_class != null)) {
      return (strContainsElem (inv_class.getName(), debugTrackClass));
    }
    return (true);
  }

  /**
   * Returns whether onot the specified ppt matches the ppts being tracked
   */
  public static boolean ppt_match (Ppt ppt) {

    if (debugTrackPpt.length > 0) {
      return ((ppt != null) && strContainsElem (ppt.name(), debugTrackPpt));
    }
    return (true);
  }

  /**
   * Returns whether or not the specified vars match the ones being tracked.
   * Also, sets Debug.ourvars with the names of the variables matched if they
   * are not the leader of their equality sets
   */

  public static boolean var_match (VarInfo[] vis) {

    if (debugTrackVars.length == 0)
      return (true);
    if (vis == null)
      return (false);

    boolean match = false;

    // Loop through each set of specified debug variables.
    outer: for (String[] cv : debugTrackVars) {
      if (cv.length != vis.length)
        continue;
      for (int j = 0; j < ourvars.length; j++)
        ourvars[j] = null;

      // Flags to insure that we don't match a variable more than once
      boolean[] used = {false, false, false};

      // Loop through each variable in this set of debug variables
      for (int j = 0; j < cv.length; j++) {
        boolean this_match = false;

        // Loop through each variable at this point
        eachvis: for (int k = 0; k < vis.length; k++) {

          // Get the matching equality set
          Set<VarInfo> evars = null;
          if (vis[k].equalitySet != null)
            evars = vis[k].equalitySet.getVars();

          // If there is an equality set
          if ((evars != null) && vis[k].isCanonical()) {

            // Loop through each variable in the equality set
            for (VarInfo v : evars) {
              if (!used[k] &&
                  (cv[j].equals ("*") || cv[j].equals (v.name()))) {
                used[k] = true;
                this_match = true;
                if (!cv[j].equals (vis[j].name())) {
                  ourvars[j] = v.name();
                  if (j != k)
                    ourvars[j] += " (" + j +"/" + k + ")";
                  if (v.isCanonical())
                    ourvars[j] += " (Leader)";
                }
                break eachvis;
              }
            }
          } else { // sometimes, no equality set
            if (cv[j].equals ("*") || cv[j].equals (vis[k].name()))
              this_match = true;
          }
        }
        if (!this_match)
          continue outer;
      }
      match = true;
      break outer;
    }

    return (match);
  }


  /**
   * Looks for an element in arr that is a substring of str.
   */
  private static boolean strContainsElem (String str, String[] arr) {

    for (String elt : arr) {
      if (str.indexOf (elt) >= 0)
        return (true);
    }
    return (false);
  }

  /**
   * Looks through entire ppt tree and checks for any items we are interested
   * in.  If found, prints them out.
   */
  public static void check (PptMap all_ppts, String msg) {

    boolean found = false;

    for (Iterator<PptTopLevel> i = all_ppts.ppt_all_iterator(); i.hasNext(); ) {
      PptTopLevel ppt = i.next();
      if (ppt_match (ppt))
        debugTrack.fine ("Matched ppt '" + ppt.name() + "' at " + msg);
      for (Iterator<PptSlice> j = ppt.views_iterator(); j.hasNext(); ) {
        PptSlice slice = j.next();
        for (int k = 0; k < slice.invs.size(); k++ ) {
          Invariant inv = slice.invs.get(k);
          if (inv.log (msg + ": found #" + k + "= " + inv.format() +
                       " in slice " + slice))
            found = true;
        }
      }
    }
    if (!found)
      debugTrack.fine ("Found no points at '" + msg + "'");
  }

  /**
   * Returns a string containing the integer variables and their
   * values
   */
  public static String int_vars (PptTopLevel ppt, ValueTuple vt) {

    String out = "";

    for (VarInfo v : ppt.var_infos) {
      if (!v.isCanonical())
        continue;
      if (v.file_rep_type != ProglangType.INT)
        continue;
      out += v.name() + "=" + toString (v.getValue(vt))
        + " [" + vt.getModified(v) + "]: ";
    }
    return out;
  }

  /**
   * Returns a string containing the variable values for any variables
   * that are currently being tracked in ppt.  The string is of the
   * form 'v1 = val1: v2 = val2, etc.
   */
  public static String related_vars (PptTopLevel ppt, ValueTuple vt) {

    String out = "";

    for (VarInfo v : ppt.var_infos) {
      for (String[] cv : debugTrackVars) {
        for (String cv_elt : cv) {
          if (cv_elt.equals (v.name())) {
            Object val = v.getValue (vt);
            int mod = vt.getModified (v);
            out += v.name() + "=";
            out += toString (val);
            if ((mod == ValueTuple.MISSING_FLOW)
              || (mod == ValueTuple.MISSING_NONSENSICAL))
              out += " (missing)";
            if (v.missingOutOfBounds())
              out += " (out of bounds)";
            if (v.equalitySet != null) {
              if (!v.isCanonical())
                out += " (leader=" + v.canonicalRep().name() + ")";
            }
            // out += " mod=" + mod;
            out += ": ";
          }
        }
      }
    }

    return (out);
  }

  public static String toString (Object val) {
    if (val == null)
      return ("none");
    if (val instanceof String)
      return "\"" + val + "\"";
    if (val instanceof long[])
      return ArraysMDE.toString ((long[])val);
    else if (val instanceof String[])
      return ArraysMDE.toString ((String[])val);
    else if (val instanceof double[])
      return ArraysMDE.toString ((double[])val);
    else if (val instanceof VarInfo[])
      return VarInfo.toString((VarInfo[]) val);
    else
      return (val.toString());
  }

  public static String toString (VarInfo[] vis) {

    String vars = "";
    for (VarInfo vi : vis)
      vars += vi.name() + " ";
    return (vars);
  }

  /**
   * Returns a string containing each variable and its value
   * The string is of the form v1 = val1: v2 = val2, etc.
   */
  public static String toString (VarInfo[] vis, ValueTuple vt) {

    String out = "";

    for (VarInfo v : vis) {
      Object val = v.getValue (vt);
      int mod = vt.getModified (v);
      out += v.name() + "=";
      out += toString (val);
      if (v.isMissing (vt))
        out += " (missing)";
      if (v.missingOutOfBounds())
        out += " (out of bounds)";
      if (v.equalitySet != null) {
       if (!v.isCanonical())
         out += " (leader=" + v.canonicalRep().name() + ")";
      }
      out += ": ";
    }

    return (out);
  }

  /**
   * Parses the specified argument to --track and sets up the track arrays
   * accordingly.  The syntax of the argument is
   *
   *    class|class|...<var,var,var>@ppt
   *
   * As shown, multiple class arguments can be specified separated by pipe
   * symbols (|).  The variables are specified in angle brackets (<>) and
   * the program point is preceeded by an at sign (@).  Each is optional
   * and can be left out.  The add_track routine can be called multiple times.
   * An invariant that matches any of the specifications will be tracked.
   */
  public static String add_track (String def) {

    String classes = null;
    String vars = null;
    String ppt = null;

    // Get the classes, vars, and ppt
    int var_start = def.indexOf ('<');
    int ppt_start = def.indexOf ("@");
    if ((var_start == -1) && (ppt_start == -1))
      classes = def;
    else if (var_start != -1) {
      if (var_start > 0)
        classes = def.substring (0, var_start);
      if (ppt_start == -1)
        vars = def.substring (var_start+1, def.length()-1);
      else {
        vars = def.substring (var_start+1, ppt_start-1);
        ppt = def.substring (ppt_start+1, def.length());
      }
    } else {
      if (ppt_start > 0)
        classes = def.substring (0, ppt_start);
      ppt = def.substring (ppt_start+1, def.length());
    }

    // If classes were specified, get each class
    if (classes != null) {
      String[] class_arr = classes.split ("\\|");
      debugTrackClass = ArraysMDE.concat (debugTrackClass, class_arr);
    }

    // If vars were specified, get each var
    if (vars != null) {
      String[] var_arr = vars.split (", *");
      String[][] new_var = new String[debugTrackVars.length+1][];
      for (int ii = 0; ii < debugTrackVars.length; ii++)
        new_var[ii] = debugTrackVars[ii];
      new_var[debugTrackVars.length] = var_arr;
      debugTrackVars = new_var;
    }

    // if a ppt was specified, add it to the array of tracked ppts
    if (ppt != null)
      debugTrackPpt = ArraysMDE.concat (debugTrackPpt, new String[] {ppt});

    System.out.println ();
    debugTrack.fine ("After --track: " + def);
    debugTrack.fine ("Track Classes: "
                     + ArraysMDE.toString (debugTrackClass, false));
    String vars_out = "";
    for (int ii = 0; ii < debugTrackVars.length; ii++)
        vars_out += ArraysMDE.toString(debugTrackVars[ii]) + " ";
    debugTrack.fine ("Track Vars: " + vars_out);
    debugTrack.fine ("Track Ppts: "
                     + ArraysMDE.toString (debugTrackPpt, false));

    return null;
  }
}
