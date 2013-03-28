package daikon.inv;

import daikon.*;
import daikon.Debug;
import daikon.inv.unary.*;
import daikon.inv.binary.*;
import daikon.inv.ternary.*;
import daikon.inv.ternary.threeScalar.*;
import daikon.inv.filter.*;
import daikon.suppress.*;
import daikon.simplify.SimpUtil;
import daikon.simplify.LemmaStack;

import utilMDE.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.*;
import java.util.*;
import java.io.Serializable;

/**
 * Base implementation for Invariant objects.
 * Intended to be subclassed but not to be directly instantiated.
 * Rules/assumptions for invariants:
 *
 * <li> For each program point's set of VarInfos, there exists exactly
 * no more than one invariant of its type.  For example, between
 * variables a and b at PptTopLevel T, there will not be two instances
 * of invariant I(a, b).
 **/
public abstract class Invariant
  implements Serializable, Cloneable // but don't YOU clone it
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20040921L;

  /**
   * General debug tracer.
   **/
  public static final Logger debug = Logger.getLogger("daikon.inv.Invariant");

  /**
   * Debug tracer for printing invariants.
   **/
  public static final Logger debugPrint = Logger.getLogger ("daikon.print");

  /**
   * Debug tracer for invariant flow.
   **/
  public static final Logger debugFlow = Logger.getLogger ("daikon.flow.flow");

  /**
   * Debug tracer for printing equality invariants.
   **/
  public static final Logger debugPrintEquality = Logger.getLogger ("daikon.print.equality");

  /**
   * Debug tracer for isWorthPrinting() checks.
   **/
  public static final Logger debugIsWorthPrinting = Logger.getLogger("daikon.print.isWorthPrinting");

  /**
   * Debug tracer for guarding.
   **/
  public static final Logger debugGuarding = Logger.getLogger("daikon.guard");

  /**
   * Debug tracer for isObvious checks.
   **/
  public static final Logger debugIsObvious = Logger.getLogger("daikon.inv.Invariant.isObvious");

  /**
   * Floating-point number between 0 and 1.  Invariants are displayed only if
   * the confidence that the invariant did not occur by chance is
   * greater than this.  (May also be set
   * via <samp>--conf_limit</samp> switch to Daikon; refer to manual.)
   **/
  public static double dkconfig_confidence_limit = .99;

  /**
   * A boolean value.  If true, Daikon's Simplify output (printed when
   * the <samp>--format simplify</samp> flag is enabled, and used internally by
   * <samp>--suppress_redundant</samp>)
   * will include new predicates representing
   * some complex relationships in invariants, such as lexical
   * ordering among sequences.  If false, some complex relationships
   * will appear in the output as complex quantified formulas, while
   * others will not appear at all.  When enabled, Simplify may be able
   * to make more inferences, allowing <samp>--suppress_redundant</samp> to
   * suppress more redundant invariants, but Simplify may also run
   * more slowly.
   **/
  public static boolean dkconfig_simplify_define_predicates = false;

  /**
   * Floating-point number between 0 and 0.1, representing the maximum
   * relative difference
   * between two floats for fuzzy comparisons.  Larger values will
   * result in floats that are relatively farther apart being treated
   * as equal.  A value of 0 essentially disables fuzzy comparisons.
   * Specifically, if <code>abs (1 - f1/f2)</code> is less than or equal
   * to this value, then the two doubles (<code>f1</code> and <code>f2</code>)
   * will be treated as equal by
   * Daikon.
   */
  public static double dkconfig_fuzzy_ratio = 0.0001;


  /**
   * The program point for this invariant, includes values, number of
   * samples, VarInfos, etc.
   **/
  public PptSlice ppt;

  // Has to be public so wrappers can read it.
  /**
   * True exactly if the invariant has been falsified:  it is guaranteed
   * never to hold (and should be either in the process of being destroyed
   * or about to be destroyed.  This should never be set directly; instead,
   * call destroy().
   **/
  protected boolean falsified = false;

  // Whether an invariant is a guarding predicate, that is, creately solely
  // for the purpose of ensuring invariants with variables that can be
  // missing do not cause exceptions when tested.  If this is true, then
  // the invariant itself does not hold over the observed data.
  public boolean isGuardingPredicate = false;

  /**
   * The probability that this could have happened by chance alone. <br>
   *   1 = could never have happened by chance; that is, we are fully confident
   *       that this invariant is a real invariant
   **/
  public static final double CONFIDENCE_JUSTIFIED = 1;

  /**
   * (0..1) = greater to lesser likelihood of coincidence <br>
   *      0 = must have happened by chance
   **/
  public static final double CONFIDENCE_UNJUSTIFIED = 0;

  /**
   * -1 = delete this invariant; we know it's not true
   **/
  public static final double CONFIDENCE_NEVER = -1;


  /**
   * The probability that this could have happened by chance alone. <br>
   *   0 = could never have happened by chance; that is, we are fully confident
   *       that this invariant is a real invariant
   **/
  public static final double PROBABILITY_JUSTIFIED = 0;

  /**
   * (0..1) = lesser to greater likelihood of coincidence <br>
   *      1 = must have happened by chance
   **/
  public static final double PROBABILITY_UNJUSTIFIED = 1;

  /**
   * 3 = delete this invariant; we know it's not true
   **/
  public static final double PROBABILITY_NEVER = 3;

  /**
   * Return Invariant.CONFIDENCE_JUSTIFIED if x>=goal.
   * Return Invariant.CONFIDENCE_UNJUSTIFIED if x<=1.
   * For intermediate inputs, the result gives confidence that grades
   * between the two extremes.
   * See the discussion of gradual vs. sudden confidence transitions.
   **/
  public static final double conf_is_ge(double x, double goal) {
    if (x>=goal)
      return 1;
    if (x<=1)
      return 0;
    double result = 1 - (goal - x)/(goal-1);
    Assert.assertTrue(0 <= result && result <= 1, "conf_is_ge: bad result = " + result + " for (x=" + x + ", goal=" + goal + ")");
    return result;
  }

  /**
   * Return Invariant.PROBABILITY_JUSTIFIED if x>=goal.
   * Return Invariant.PROBABILITY_UNJUSTIFIED if x<=1.
   * For intermediate inputs, the result gives probability that grades
   * between the two extremes.
   * See the discussion of gradual vs. sudden probability transitions.
   **/
  public static final double prob_is_ge(double x, double goal) {
    if (x>=goal)
      return 0;
    if (x<=1)
      return 1;
    double result = (goal - x)/(goal-1);
    Assert.assertTrue(0 <= result && result <= 1, "prob_is_ge: bad result = " + result + " for (x=" + x + ", goal=" + goal + ")");
    return result;
  }


  /** Return the confidence that both conditions are satisfied. */
  public static final double confidence_and(double c1, double c2) {
    Assert.assertTrue(0 <= c1 && c1 <= 1, "confidence_and: bad c1 = " + c1);
    Assert.assertTrue(0 <= c2 && c2 <= 1, "confidence_and: bad c2 = " + c2);

    double result = c1*c2;

    Assert.assertTrue(0 <= result && result <= 1, "confidence_and: bad result = " + result);
    return result;
  }

  /** Return the confidence that all three conditions are satisfied. */
  public static final double confidence_and(double c1, double c2, double c3) {
    Assert.assertTrue(0 <= c1 && c1 <= 1, "confidence_and: bad c1 = " + c1);
    Assert.assertTrue(0 <= c2 && c2 <= 1, "confidence_and: bad c2 = " + c1);
    Assert.assertTrue(0 <= c3 && c3 <= 1, "confidence_and: bad c3 = " + c1);

    double result =  c1*c2*c3;

    Assert.assertTrue(0 <= result && result <= 1, "confidence_and: bad result = " + result);
    return result;
  }

  /** Return the confidence that either condition is satisfied. */
  public static final double confidence_or(double c1, double c2) {
    // Not "1-(1-c1)*(1-c2)" because that can produce a value too large; we
    // don't want the result to be larger than the larger argument.
    return Math.max(c1, c2);
  }


  /** Return the probability that both conditions are satisfied. */
  public static final double prob_and(double p1, double p2) {
    Assert.assertTrue(0 <= p1 && p1 <= 1, "prob_and: bad p1 = " + p1);
    Assert.assertTrue(0 <= p2 && p2 <= 1, "prob_and: bad p2 = " + p2);

    // 1 - (1-p1)*(1-p2)
    double result = p1 + p2 - p1*p2;

    Assert.assertTrue(0 <= result && result <= 1, "prob_and: bad result = " + result);
    return result;
  }

  /** Return the probability that all three conditions are satisfied. */
  public static final double prob_and(double p1, double p2, double p3) {
    Assert.assertTrue(0 <= p1 && p1 <= 1, "prob_and: bad p1 = " + p1);
    Assert.assertTrue(0 <= p2 && p2 <= 1, "prob_and: bad p2 = " + p1);
    Assert.assertTrue(0 <= p3 && p3 <= 1, "prob_and: bad p3 = " + p1);

    double result =  1 - (1 - p1) * (1 - p2) * (1 - p3);

    Assert.assertTrue(0 <= result && result <= 1, "prob_and: bad result = " + result);
    return result;
  }

  /** Return the probability that either condition is satisfied. */
  public static final double prob_or(double p1, double p2) {
    // Not "p1*p2" because that can produce a value too small; we don't
    // want the result to be smaller than the smaller argument.
    return Math.min(p1, p2);
  }


  // Subclasses should set these; Invariant never does.

  /**
   * At least this many samples are required, or else we don't report any
   * invariant at all.  (Except that OneOf invariants are treated differently.)
   **/
  public static final int min_mod_non_missing_samples = 5;

  /**
   * @return true if the invariant has enough samples to have its
   * computed constants well-formed.  Is overridden in classes like
   * LinearBinary/Ternary and Upper/LowerBound.
   **/
  public boolean enoughSamples() {
    return true;
  }


  // The confidence routines (getConfidence and internal helper
  // computeConfidence) use ModBitTracker information to compute
  // justification.

  // There are three confidence routines:
  //  justified() is what most clients should call
  //  getConfidence() gives the actual confidence.  It used to cache
  //    results, but it does not do so any longer.
  //  computeConfidence() is an internal helper method that does the
  //    actual work, but it should not be called externally, only by
  //    getConfidence.  It ignores whether the invariant is falsified.

  // There are two general approaches to computing confidence
  // when there is a threshold (such as needing to see 10 samples):
  //  * Make the confidence typically either 0 or 1, transitioning
  //    suddenly between the two as soon as the 10th sample is observed.
  //  * Make the confidence transition more gradually; for instance, each
  //    sample changes the confidence by 10%.
  // The gradual approach has advantages and disadvantages:
  //  + Users can set the confidence limit to see invariants earlier; this
  //    is simpler than figuring out all the thresholds to set.
  //  + Tools such as the operational difference for test suite generation
  //    are assisted by knowing whether they are getting closer to
  //    justification.
  //  - The code is a bit more complicated.


  /** A wrapper around getConfidence() or getConfidence(). **/
  public final boolean justified() {
    boolean just = (!falsified
                    && (getConfidence() >= dkconfig_confidence_limit));
    if (logOn())
      log ("justified = " + just + ", confidence = " + getConfidence());
    return (just);
  }

  // If confidence == CONFIDENCE_NEVER, then this invariant can be eliminated.
  /**
   * Given that this invariant has been true for all values seen so far,
   * this method returns the confidence that that situation has occurred
   * by chance alone.  The result is a value between 0 and 1 inclusive.  0
   * means that this invariant could never have occurred by chance alone;
   * we are fully confident that its truth is no coincidence.  1 means that
   * the invariant is certainly a happenstance, so the truth of the
   * invariant is not relevant and it should not be reported.  Values
   * between 0 and 1 give differing confidences in the invariant.
   * <p>
   *
   * As an example, if the invariant is "x!=0", and only one value, 22, has
   * been seen for x, then the conclusion "x!=0" is not justified.  But if
   * there have been 1,000,000 values, and none of them were 0, then we may
   * be confident that the property "x!=0" is not a coincidence.
   * <p>
   *
   * This method need not check the value of field "falsified", as the
   * caller does that.
   * <p>
   *
   * This method is a wrapper around computeConfidence(), which does the
   * actual work.
   * @see #computeConfidence()
   **/
  public final double getConfidence() {
    Assert.assertTrue(! falsified);
    // if (falsified)
    //   return CONFIDENCE_NEVER;
    double result = computeConfidence();
    // System.out.println("getConfidence: " + getClass().getName() + " " + ppt.varNames());
    if (!((result == CONFIDENCE_JUSTIFIED)
          || (result == CONFIDENCE_UNJUSTIFIED)
          || (result == CONFIDENCE_NEVER)
          || ((0 <= result) && (result <= 1)))) {
      // Can't print this.repr_prob(), as it may compute the confidence!
      System.out.println("getConfidence: " + getClass().getName() + " " + ppt.varNames() + " => " + result);
      System.out.println("  " + this.format() + "; " + repr());
    }
    Assert.assertTrue(((0 <= result) && (result <= 1))
                      || (result == CONFIDENCE_JUSTIFIED)
                      || (result == CONFIDENCE_UNJUSTIFIED)
                      || (result == CONFIDENCE_NEVER)
                      // This can be expensive, so comment out.
                      // , getClass().getName() + ": " + repr() + ", result=" + result
                      , "unexpected conf value: " + result);
    return result;
  }

  /**
   * This method computes the confidence that this invariant occurred by chance.
   * Users should use getConfidence() instead.
   * @see     #getConfidence()
   **/
  protected abstract double computeConfidence();

  /**
   * Subclasses should override.  An exact invariant indicates that given
   * all but one variable value, the last one can be computed.  (I think
   * that's correct, anyway.)  Examples are IntComparison (when only
   * equality is possible), LinearBinary, FunctionUnary.
   * OneOf is treated differently, as an interface.
   * The result of this method does not depend on whether the invariant is
   * justified, destroyed, etc.
   **/
  public boolean isExact() {
    return false;
  }

  // Implementations of this need to examine all the data values already
  // in the ppt.  Or, don't put too much work in the constructor and instead
  // have the caller do that.
  // The "ppt" argument can be null if this is a prototype invariant.
  protected Invariant(PptSlice ppt) {
    this.ppt = ppt;
  }

  /**
   * Marks the invariant as falsified.  Should always be called rather
   * than just setting the flag so that we can track when this happens
   */
  public void falsify() {
    falsified = true;
    if (logOn())
      log ("Destroyed " + format());
  }

  /** Clear the falsified flag. */
  public void clear_falsified() {
    falsified = false;
  }

  /** Returns whether or not this invariant has been destroyed. */
  public boolean is_false() {
    return (falsified);
  }

  /**
   * Do nothing special, Overridden to remove
   * exception from declaration
   **/
  public Invariant clone() {
    try {
      Invariant result = (Invariant) super.clone();
      return result;
    } catch (CloneNotSupportedException e) {
      throw new Error(); // can never happen
    }
  }

  /**
   * Take an invariant and transfer it into a new PptSlice.
   * @param new_ppt must have the same arity and types
   * @param permutation gives the varinfo array index mapping in the
   * new ppt
   **/
  public Invariant transfer(PptSlice new_ppt,
                            int[] permutation
                            ) {
    // Check some sanity conditions
    Assert.assertTrue(new_ppt.arity() == ppt.arity());
    Assert.assertTrue(permutation.length == ppt.arity());
    for (int i=0; i < ppt.arity(); i++) {
      VarInfo oldvi = ppt.var_infos[i];
      VarInfo newvi = new_ppt.var_infos[permutation[i]];
      // We used to check that all 3 types were equal, but we can't do
      // that anymore, because with equality, invariants may get
      // transferred between old and new VarInfos of different types.
      // They are, however, comparable
      Assert.assertTrue (oldvi.comparableNWay(newvi));
    }

    Invariant result;
    // Clone it
    result = this.clone();

    // Fix up the fields
    result.ppt = new_ppt;
    // Let subclasses fix what they need to
    result = result.resurrect_done(permutation);

    if (logOn()) {
      result.log ("Created " + result.getClass().getName() + ":"
                  + result.format() + " via transfer from "
                  + getClass().getName() + ":" + format()
                  + " using permutation "
                  + ArraysMDE.toString (permutation)
                  + " old_ppt = " + ppt
                  + " new_ppt = " + new_ppt);
      // result.log (UtilMDE.backTrace());
    }
    //if (debug.isLoggable(Level.FINE))
    //    debug.fine ("Invariant.transfer to " + new_ppt.name() + " "
    //                 + result.repr());

    return result;
  }

  /**
   * Clones the invariant and then permutes it as specified.  Normally
   * used to make child invariant match the variable order of the parent
   * when merging invariants bottom up.
   */
  public Invariant clone_and_permute (int[] permutation) {

    Invariant result = this.clone();
    result = result.resurrect_done (permutation);

    if (logOn())
      result.log ("Created " + result.format() + " via clone_and_permute from "
                  + format() + " using permutation "
                  + ArraysMDE.toString (permutation)
                  + " old_ppt = " + VarInfo.toString (ppt.var_infos)
                  // + " new_ppt = " + VarInfo.toString (new_ppt.var_infos)
                  );

    return (result);
  }

  /**
   * Take a falsified invariant and resurrect it in a new PptSlice.
   * @param new_ppt must have the same arity and types
   * @param permutation gives the varinfo array index mapping
   **/
  public Invariant resurrect(PptSlice new_ppt,
                             int[] permutation
                             ) {
    // Check some sanity conditions
    Assert.assertTrue(falsified);
    Assert.assertTrue(new_ppt.arity() == ppt.arity());
    Assert.assertTrue(permutation.length == ppt.arity());
    for (int i=0; i < ppt.arity(); i++) {
      VarInfo oldvi = ppt.var_infos[i];
      VarInfo newvi = new_ppt.var_infos[permutation[i]];
      // We used to check that all 3 types were equal, but we can't do
      // that anymore, because with equality, invariants may get
      // resurrected between old and new VarInfos of different types.
      // They are, however, comparable
      Assert.assertTrue(oldvi.comparableNWay(newvi));
    }

    Invariant result;
    // Clone it
    result = this.clone();

    // Fix up the fields
    result.falsified = false;
    result.ppt = new_ppt;
    // Let subclasses fix what they need to
    result = result.resurrect_done(permutation);

    if (logOn())
      result.log ("Created " + result.format() + " via resurrect from "
                  + format() + " using permutation "
                  + ArraysMDE.toString (permutation)
                  + " old_ppt = " + VarInfo.toString (ppt.var_infos)
                  + " new_ppt = " + VarInfo.toString (new_ppt.var_infos));

    return result;
  }

  /**
   * Returns a single VarComparability that describes the set of
   * variables used by this invariant.  Since all of the variables
   * in an invariant must be comparable, this can usually be the
   * comparability information for any variable.  The exception is
   * when one or more variables is always comparable (comparable to
   * everythign else).  An always comparable VarComparability is
   * returned only if all of the variables involved are always
   * comparable.  Otherwise the comparability information from one
   * of the non always-comparable variables is returned.
   */
  public VarComparability get_comparability() {

    // Assert.assertTrue (ppt != null, "class " + getClass());

    // Return the first variable that is not always-comparable
    for (int i = 0; i < ppt.var_infos.length; i++) {
      VarComparability vc = ppt.var_infos[i].comparability;
      if (!vc.alwaysComparable())
        return (vc);
    }

    // All the variables are always-comparable, just return the first one
   // return (ppt.var_infos[0].comparability);
   return VarComparabilityImplicit.unknown;
  }

  /**
   * Merge the invariants in invs to form a new invariant.  This implementation
   * merely returns a clone of the first invariant in the list.  This is
   * correct for simple invariants whose equation or statistics don't depend
   * on the actual samples seen.  It should be overriden for more complex
   * invariants (eg, bound, oneof, linearbinary, etc).
   *
   * @param invs        List of invariants to merge.  The invariants must
   *                    all be of the same type and should come from
   *                    the children of parent_ppt.  They should also all be
   *                    permuted to match the variable order in parent_ppt.
   * @param parent_ppt  Slice that will contain the new invariant
   *
   * @return the merged invariant or null if the invariants didn't represent
   * the same invariant.
   */
  public Invariant merge (List<Invariant> invs, PptSlice parent_ppt) {

    Invariant first = invs.get(0);
    Invariant result = first.clone();
    result.ppt = parent_ppt;
    result.log ("Merged '" + result.format() + "' from " + invs.size()
                + " child invariants " /* + first.ppt.name() */);

    // Make sure that each invariant was really of the same type
    if (Assert.enabled) {
      Match m = new Match (result);
      for (int i = 1; i < invs.size(); i++ )
        Assert.assertTrue (m.equals (new Match (invs.get(i))));
    }

    return (result);

  }

  /**
   * Permutes the invariant as specified.  Often creates a new invariant
   * (with a different class)
   */
  public Invariant permute (int[] permutation) {
    return (resurrect_done (permutation));
  }

  /**
   * Called on the new invariant just before resurrect() returns it to
   * allow subclasses to fix any information they might have cached
   * from the old Ppt and VarInfos.
   **/
  protected abstract Invariant resurrect_done(int[] permutation);

  // Regrettably, I can't declare a static abstract method.
  // // The return value is probably ignored.  The new Invariant installs
  // // itself on the PptSlice, and that's what really matters (right?).
  // public static abstract Invariant instantiate(PptSlice ppt);

  public boolean usesVar(VarInfo vi) {
    return ppt.usesVar(vi);
  }

  public boolean usesVar(String name) {
    return ppt.usesVar(name);
  }

  public boolean usesVarDerived(String name) {
    return ppt.usesVarDerived(name);
  }

  // Not used as of 1/31/2000
  // // For use by subclasses.
  // /** Put a string representation of the variable names in the StringBuffer. */
  // public void varNames(StringBuffer sb) {
  //   // sb.append(this.getClass().getName());
  //   ppt.varNames(sb);
  // }

  /** Return a string representation of the variable names. */
  public final String varNames() {
    return ppt.varNames();
  }

  // repr()'s output should not include result of getConfidence, because
  // repr() may be called from computeConfidence or elsewhere for
  // debugging purposes.
  /**
   * For printing invariants, there are two interfaces:
   * repr gives a low-level representation
   * (repr_prop also prints the confidence), and
   * format gives a high-level representation for user output.
   **/
  public String repr() {
    // A better default would be to use reflection and print out all
    // the variable names.
    return getClass() + varNames() + ": " + format();
  }

  /**
   * For printing invariants, there are two interfaces:
   * repr gives a low-level representation
   * (repr_prop also prints the confidence), and
   * format gives a high-level representation for user output.
   **/
  public String repr_prob() {
    return repr()
      + "; confidence = " + getConfidence()
      ;
  }

  /**
   * For printing invariants, there are two interfaces:
   * repr gives a low-level representation
   * (repr_prop also prints the confidence), and
   * format gives a high-level representation for user output.
   **/
  public String format() {
    String result = format_using(OutputFormat.DAIKON);
    if (PrintInvariants.dkconfig_print_inv_class) {
      String classname = getClass().getName();
      int index = classname.lastIndexOf('.');
      classname = classname.substring(index+1);
      result = result + " [" + classname + "]";
    }
    return result;
  }

  public abstract String format_using(OutputFormat format);

  /**
   * @return conjuction of mapping the same function of our
   * expresssions's VarInfos, in general.  Subclasses may override if
   * they are able to handle generally-inexpressible properties in
   * special-case ways.
   *
   * @see VarInfo#isValidEscExpression
   **/
  public boolean isValidEscExpression() {
    for (int i=0; i < ppt.var_infos.length; i++) {
      if (! ppt.var_infos[i].isValidEscExpression()) {
        return false;
      }
    }
    return true;
  }

  /** A "\type(...)" construct where the "..." contains a "$". **/
  private static Pattern anontype_pat = Pattern.compile("\\\\type\\([^\\)]*\\$");

  /**
   * @return true if this Invariant can be properly formatted for Java output.
   **/
  public boolean isValidExpression(OutputFormat format) {
    if ((format == OutputFormat.ESCJAVA) && (! isValidEscExpression())) {
      return false;
    }

    String s = format_using(format);

    if ((format == OutputFormat.ESCJAVA) || format.isJavaFamily()) {
      // This list should get shorter as we improve the formatting.
      if ((s.indexOf(" needs to be implemented: ") != -1)
          || (s.indexOf("<<") != -1)
          || (s.indexOf(">>") != -1)
          || (s.indexOf("warning: ") != -1)
          || (s.indexOf('~') != -1)
          || (s.indexOf("\\new") != -1)
          || (s.indexOf(".toString ") != -1)
          || (s.endsWith(".toString"))
          || (s.indexOf(".getClass") != -1)
          || (s.indexOf(".typeArray") != -1)
          || (s.indexOf("warning: method") != -1)
          || (s.indexOf("inexpressible") != -1)
          || (s.indexOf("unimplemented") != -1)
          || (s.indexOf("Infinity") != -1)
          || anontype_pat.matcher(s).find()) {
        return false;
      }
    }
    return true;
  }


  /**
   * @return standard "format needs to be implemented" for the given
   * requested format.  Made public so cores can call it.
   **/
  public String format_unimplemented(OutputFormat request) {
    if ((request == OutputFormat.IOA) && debugPrint.isLoggable(Level.FINE)) {
      debugPrint.fine ("Format_ioa: " + this.toString());
    }
    String classname = this.getClass().getName();
    return "warning: method " + classname + ".format(" + request + ")"
      + " needs to be implemented: " + format();
  }

  /**
   * @return standard "too few samples for to have interesting
   * invariant" for the requested format. For machine-readable
   * formats, this is just "true". An optional string argument, if
   * supplied, is a human-readable description of the invariant in its
   * uninformative state, which will be added to the message.
   **/
  public String format_too_few_samples(OutputFormat request, String attempt) {
    if (request == OutputFormat.SIMPLIFY) {
      return "(AND)";
    } else if (request == OutputFormat.IOA ||
               request == OutputFormat.JAVA ||
               request == OutputFormat.ESCJAVA ||
               request == OutputFormat.JML ||
               request == OutputFormat.DBCJAVA ) {
      return "true";
    }
    String classname = this.getClass().getName();
    if (attempt == null) {
      attempt = varNames();
    }
    return "warning: too few samples for " + classname
      + " invariant: " + attempt;
  }

  /**
   * Convert a floating point value into the weird Modula-3-like
   * floating point format that the Simplify tool requires.
   */
  public static String simplify_format_double(double d) {
    String s = d + "";
    if (s.indexOf('E') != -1) {
      // 1E6 -> 1d6
      // 1.43E6 -> 1.43d6
      s = s.replace('E', 'd');
    } else if (s.indexOf('.') != -1) {
      // 3.14 -> 3.14d0
      s = s + "d0";
    } else if (s.equals("-Infinity")) {
      // -Infinity -> NegativeInfinity
      s = "NegativeInfinity";
    }
    // 5 -> 5
    // NaN -> NaN
    // Infinity -> Infinity
    return s;
  }

  /**
   * Conver a long integer value into a format that Simplify can
   * use. If the value is too big, we have to print it in a weird way,
   * then tell Simplify about its properties specially. **/
  public static String simplify_format_long(long l) {
    LemmaStack.noticeInt(l);
    if (l >= -32000 && l <= 32000) {
      // Note that the above range is actually smaller than the
      // real range of [-2147483648..2147483647], since Simplify can
      // get in trouble close to the boundary (try
      // > (BG_PUSH (< 2147483647 n))
      // to get an internal assertion failure)
      // For that matter, try
      // > (BG_PUSH (>= x -1073741825))
      // > (BG_PUSH (<= x 1073741825))
      // > (OR)
      // Or, close to the square root of the boundary:
      // > (BG_PUSH (EQ x 56312))
      // > (BG_PUSH (EQ y (* y x)))
      return "" + l;
    } else {
      return SimpUtil.formatInteger(l);
    }
  }

  /**
   * Convert a string value into the weird |-quoted format that the
   * Simplify tool requires. (Note that Simplify doesn't distinguish
   * between variables, symbolic constants, and strings, so we prepend
   * "_string_" to avoid collisions with variables and other symbols).
   */
  public static String simplify_format_string(String s) {
    if (s == null)
      return "null";
    StringBuffer buf = new StringBuffer("|_string_");
    if (s.length() > 150) {
      // Simplify can't handle long strings (its input routines have a
      // 4000-character limit for |...| identifiers, but it gets an
      // internal array overflow for ones more than about 195
      // characters), so replace all but the beginning and end of a
      // long string with a hashed summary.
      int summ_length = s.length() - 100;
      int p1 = 50 + summ_length / 4;
      int p2 = 50 + summ_length / 2;
      int p3 = 50 + 3 * summ_length / 4;
      int p4 = 50 + summ_length;
      StringBuffer summ_buf = new StringBuffer(s.substring(0, 50));
      summ_buf.append("...");
      summ_buf.append(Integer.toHexString(s.substring(50, p1).hashCode()));
      summ_buf.append(Integer.toHexString(s.substring(p1, p2).hashCode()));
      summ_buf.append(Integer.toHexString(s.substring(p2, p3).hashCode()));
      summ_buf.append(Integer.toHexString(s.substring(p3, p4).hashCode()));
      summ_buf.append("...");
      summ_buf.append(s.substring(p4));
      s = summ_buf.toString();
    }
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\n')            // not lineSep
        buf.append("\\n");      // not lineSep
      else if (c == '\r')
        buf.append("\\r");
      else if (c == '\t')
        buf.append("\\t");
      else if (c == '\f')
        buf.append("\\f");
      else if (c == '\\')
        buf.append("\\\\");
      else if (c == '|')
        buf.append("\\|");
      else if (c >= ' ' && c <= '~')
        buf.append(c);
      else {
        buf.append("\\");
        // AFAIK, Simplify doesn't glork Unicode, so lop off all but
        // the low 8 bits
        String octal = Integer.toOctalString(c & 0xff);
        // Also, Simplify only accepts octal escapes with exactly 3 digits
        while (octal.length() < 3)
          octal = "0" + octal;
        buf.append(octal);
      }
    }
    buf.append("|");
    return buf.toString();
  }

  // This should perhaps be merged with some kind of PptSlice comparator.
  /**
   * Compare based on arity, then printed representation.
   **/
  public static final class InvariantComparatorForPrinting implements Comparator<Invariant> {
    public int compare(Invariant inv1, Invariant inv2) {
      if (inv1 == inv2)
        return 0;

      // Guarding implications should compare as if they were without the
      // guarding predicate

      if (inv1 instanceof GuardingImplication)
        inv1 = ((GuardingImplication)inv1).right;
      if (inv2 instanceof GuardingImplication)
        inv2 = ((GuardingImplication)inv2).right;

      // Put equality invariants first
      if ((inv1 instanceof Comparison) && (! (inv2 instanceof Comparison)))
        return -1;
      if ((! (inv1 instanceof Comparison)) && (inv2 instanceof Comparison))
        return 1;

      // Assert.assertTrue(inv1.ppt.parent == inv2.ppt.parent);
      VarInfo[] vis1 = inv1.ppt.var_infos;
      VarInfo[] vis2 = inv2.ppt.var_infos;
      int arity_cmp = vis1.length - vis2.length;
      if (arity_cmp != 0)
        return arity_cmp;
      // Comparing on variable index is wrong in general:  variables of the
      // same name may have different indices at different program points.
      // However, it's safe if the invariants are from the same program
      // point.  Also, it is nice to avoid changing the order of variables
      // from that of the data trace file.

      if (inv1.ppt.parent == inv2.ppt.parent) {
        for (int i=0; i<vis1.length; i++) {
          int tmp = vis1[i].varinfo_index - vis2[i].varinfo_index;
          if (tmp != 0) {
            return tmp;
          }
        }
      } else {
        // // Debugging
        // System.out.println("ICFP: different parents for " + inv1.format() + ", " + inv2.format());

        for (int i=0; i<vis1.length; i++) {
          String name1 = vis1[i].name();
          String name2 = vis2[i].name();
          if (name1.equals(name2)) {
            continue;
          }
          int name1in2 = inv2.ppt.parent.indexOf(name1);
          int name2in1 = inv1.ppt.parent.indexOf(name2);
          int cmp1 = (name1in2 == -1) ? 0 : vis1[i].varinfo_index - name1in2;
          int cmp2 = (name2in1 == -1) ? 0 : vis2[i].varinfo_index - name2in1;
          int cmp = MathMDE.sign(cmp1) + MathMDE.sign(cmp2);
          if (cmp != 0)
            return cmp;
        }
      }

      // Sort OneOf invariants earlier than others
      if ((inv1 instanceof OneOf) && (! (inv2 instanceof OneOf)))
        return -1;
      if ((! (inv1 instanceof OneOf)) && (inv2 instanceof OneOf))
        return 1;

      // System.out.println("ICFP: default rule yields "
      //                    + inv1.format().compareTo(inv2.format())
      //                    + " for " + inv1.format() + ", " + inv2.format());
      if (PrintInvariants.dkconfig_old_array_names && FileIO.new_decl_format)
        return inv1.format().replace ("[..]", "[]")
          .compareTo (inv2.format().replace ("[..]", "[]"));
      else
        return inv1.format().compareTo(inv2.format());
    }
  }

  /**
   * @return true iff the two invariants represent the same
   * mathematical formula.  Does not consider the context such as
   * variable names, confidences, sample counts, value counts, or
   * related quantities.  As a rule of thumb, if two invariants format
   * the same, this method returns true.  Furthermore, in many cases,
   * if an invariant does not involve computed constants (as "x&gt;c" and
   * "y=ax+b" do for constants a, b, and c), then this method vacuously
   * returns true.
   *
   * @exception RuntimeException if other.getClass() != this.getClass()
   **/
  public boolean isSameFormula(Invariant other) {
    return false;
  }

  /**
   * Returns whether or not it is possible to merge invariants of the same
   * class but with different formulas when combining invariants from lower
   * ppts to build invariants at upper program points.  Invariants that
   * have this characteristic (eg, bound, oneof) should override this
   * function.  Note that invariants that can do this, normally need special
   * merge code as well (to merge the different formulas into a single formula
   * at the upper point
   */
  public boolean mergeFormulasOk () {
    return (false);
  }

  /**
   * @return true iff the argument is the "same" invariant as this.
   * Same, in this case, means a matching type, formula, and variable
   * names.
   **/
  public boolean isSameInvariant(Invariant inv2) {
    // return isSameInvariant(inv2, defaultIsSameInvariantNameExtractor);

    Invariant inv1 = this;

    // Can't be the same if they aren't the same type
    if (!inv1.getClass().equals(inv2.getClass())) {
      return false;
    }

    // Can't be the same if they aren't the same formula
    if (!inv1.isSameFormula(inv2)) {
      return false;
    }

    // The variable names much match up, in order
    VarInfo[] vars1 = inv1.ppt.var_infos;
    VarInfo[] vars2 = inv2.ppt.var_infos;

    // due to inv type match already
    Assert.assertTrue(vars1.length == vars2.length);

    for (int i=0; i < vars1.length; i++) {
      VarInfo var1 = vars1[i];
      VarInfo var2 = vars2[i];
      if (!var1.name().equals (var2.name()))
        return false;
    }

    return true;
  }


  /**
   * @return true iff the two invariants represent mutually exclusive
   * mathematical formulas -- that is, if one of them is true, then the
   * other must be false.  This method does not consider the context such
   * as variable names, confidences, sample counts, value counts, or
   * related quantities.
   **/
  public boolean isExclusiveFormula(Invariant other) {
    return false;
  }


  /**
   * Look up a previously instantiated Invariant.
   **/
  // This implementation should be made more efficient, because it's used in
  // suppression.  We should somehow index invariants by their type.
  public static Invariant find(Class invclass, PptSlice ppt) {
    for (Invariant inv : ppt.invs) {
      if (inv.getClass() == invclass)
        return inv;
    }
    return null;
  }

  /**
   * Returns the set of non-instantiating suppressions for this invariant.
   * Should be overridden by subclasses with non-instantiating suppressions.
   */
  public NISuppressionSet get_ni_suppressions() {
    return (null);
  }

  /**
   * Returns whether or not this invariant is ni-suppressed.
   */
  public boolean is_ni_suppressed() {

    NISuppressionSet ss = get_ni_suppressions();
    if (ss == null)
      return (false);
    boolean suppressed = ss.suppressed (ppt);
    if (suppressed && Debug.logOn() && (Daikon.current_inv != null))
      Daikon.current_inv.log ("inv " + format() + " suppressed: " + ss);
    if (Debug.logDetail())
      log ("suppressed = " + suppressed + " suppression set = " + ss);

    return (suppressed);
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Tests about the invariant (for printing)
  ///

  // DO NOT OVERRIDE.  Should be declared "final", but the "final" is
  // omitted to allow for easier testing.
  public boolean isWorthPrinting() {
    return InvariantFilters.defaultFilters().shouldKeep(this) == null;
  }

  ////////////////////////////////////////////////////////////////////////////
  // Static and dynamic checks for obviousness

  /**
   * Return true if this invariant is necessarily true from a fact
   * that can be determined statically (i.e., the decls files) (e.g.,
   * by being from a certain derivation).  Intended to be overridden
   * by subclasses.
   *
   * <p> This method is final because children of Invariant should be
   * extending isObviousStatically(VarInfo[]) because it is more
   * general.
   **/
  public final DiscardInfo isObviousStatically() {
    return isObviousStatically(this.ppt.var_infos);
  }

  /**
   * Return true if this invariant is necessarily true from a fact
   * that can be determined statically -- for the given varInfos
   * rather than the varInfos of this.  Conceptually, this means "is
   * this invariant statically obvious if its VarInfos were switched
   * with vis?"  Intended to be overridden by subclasses.  Should only
   * do static checking.
   * Precondition: vis.length == this.ppt.var_infos.length
   * @param vis The VarInfos this invariant is obvious over.  The
   * position and data type of the variables is the *same* as that of
   * this.ppt.var_infos.
   **/
  public DiscardInfo isObviousStatically(VarInfo[] vis) {
    return null;
  }

  /**
   * Return true if this invariant and all equality combinations of
   * its member variables are necessarily true from a fact that can be
   * determined statically (i.e., the decls files).  For example, a ==
   * b, and f(a) is obvious, but f(b) is not.  In that case, this
   * method on f(a) would return false.  If f(b) is also obvious, then
   * this method would return true.
   **/
  // This is used because we cannot decide to non-instantiate some
  // invariants just because isObviousStatically is true, since some
  // of the member variables may be equal to non-obvious varInfos.  If
  // we were to non-instantiate, we could not copy an invariant to the
  // non-obvious VarInfos should they split off from the obvious one.
  // Of course, it's expensive to examine every possible permutation
  // of VarInfos and their equality set, so a possible conservative
  // approximation is to simply return false.
  public boolean isObviousStatically_AllInEquality() {
    // If the leaders aren't statically obvious, then clearly not all
    // combinations are.
    if (isObviousStatically() == null) return false;

    for (int i = 0; i < ppt.var_infos.length; i++) {
      if (ppt.var_infos[i].equalitySet.getVars().size() > 1) return false;
    }
    return true;
  }

  /**
   * Return true if this invariant and some equality combinations of
   * its member variables are statically obvious.  For example, if a ==
   * b, and f(a) is obvious, then so is f(b).  We use the someInEquality
   * (or least interesting) method during printing so we only print an
   * invariant if all its variables are interesting, since a single,
   * static, non interesting occurance means all the equality
   * combinations aren't interesting.
   * @return the VarInfo array that contains the VarInfos that showed
   * this invariant to be obvious.  The contains variables that are
   * elementwise in the same equality set as this.ppt.var_infos.  Can
   * be null if no such assignment exists.
   **/
  public DiscardInfo isObviousStatically_SomeInEquality() {
    DiscardInfo result = isObviousStatically();
    if (result != null) return result;
    return isObviousStatically_SomeInEqualityHelper (this.ppt.var_infos,
                                                     new VarInfo[this.ppt.var_infos.length],
                                                     0);
  }

  // TODO: finish this comment.
  /**
   * Recurse through vis and generate the cartesian product of ...
   **/
  protected DiscardInfo isObviousStatically_SomeInEqualityHelper(VarInfo[] vis,
                                                             VarInfo[] assigned,
                                                             int position) {
    if (position == vis.length) {
      if (debugIsObvious.isLoggable(Level.FINE)) {
        StringBuffer sb = new StringBuffer();
        sb.append ("  isObviousStatically_SomeInEquality: ");
        for (int i = 0; i < vis.length; i++) {
          sb.append (assigned[i].name() + " ");
        }
        debugIsObvious.fine (sb.toString());
      }

      return isObviousStatically(assigned);
    } else {
      for (VarInfo vi : vis[position].get_equalitySet_vars ()) {
        assigned[position] = vi;
        DiscardInfo temp =
          isObviousStatically_SomeInEqualityHelper (vis, assigned, position + 1);
        if (temp != null) return temp;
      }
      return null;
    }
  }

  /**
   * Return true if this invariant is necessarily true from a fact that can
   * be determined statically (i.e., the decls files) or dynamically (after
   * checking data).  Intended not to be overriden, because sub-classes
   * should override isObviousStatically or isObviousDynamically.  Wherever
   * possible, suppression, rather than this, should do the dynamic checking.
   **/
  public final DiscardInfo isObvious() {
    // Actually actually, we'll eliminate invariants as they become obvious
    // rather than on output; the point of this is to speed up computation.
    // // Actually, we do need to check isObviousDerived after all because we
    // // add invariants that might be obvious, but might also turn out to be
    // // even stronger (and so not obvious).  We don't know how the invariant
    // // turns out until after testing it.
    // // // We don't need to check isObviousDerived because we won't add
    // // // obvious-derived invariants to lists in the first place.
    DiscardInfo staticResult = isObviousStatically_SomeInEquality();
    if (staticResult != null) {
      if (debugPrint.isLoggable(Level.FINE))
        debugPrint.fine ("  [obvious:  " + repr_prob() + " ]");
      return staticResult;
    } else {
      DiscardInfo dynamicResult = isObviousDynamically_SomeInEquality();
      if (dynamicResult != null) {
        if (debugPrint.isLoggable(Level.FINE))
          debugPrint.fine ("  [obvious:  " + repr_prob() + " ]");
        return dynamicResult;
      } else {
        return null;
      }
    }
  }

  /**
   * Return non-null if this invariant is necessarily true from a fact that
   * can be determined dynamically (after checking data) -- for the given
   * varInfos rather than the varInfos of this.  Conceptually, this means,
   * "Is this invariant dynamically obvious if its VarInfos were switched
   * with vis?"  Intended to be overriden by subclasses so they can filter
   * invariants after checking; the overriding method should first call
   * "super.isObviousDynamically(vis)".  Since this method is
   * dynamic, it should only be called after all processing.
   **/
  public DiscardInfo isObviousDynamically(VarInfo[] vis) {
    Assert.assertTrue (!Daikon.isInferencing);
    Assert.assertTrue(vis.length <= 3, "Unexpected more-than-ternary invariant");
    if (! ArraysMDE.noDuplicates(vis)) {
      log ("Two or more variables are equal " + format());
      return new DiscardInfo(this, DiscardCode.obvious,
                             "Two or more variables are equal");
    }
    // System.out.println("Passed Invariant.isObviousDynamically(): " + format());
    return null;
  }

  /**
   * Return true if more than one of the variables in the invariant
   * are the same variable. We create such invariants for the purpose
   * of equality set processing, but they aren't intended for
   * printing; there should be invariants with the same meaning but
   * lower arity instead. For instance, we don't need "x = x + x"
   * because we have "x = 0" instead.
   *
   * Actually, this isn't strictly true: we don't have an invariant
   * "a[] is a palindrome" corresponding to "a[] is the reverse of
   * a[]", for instance.
   **/
  public boolean isReflexive() {
    return ! ArraysMDE.noDuplicates(ppt.var_infos);
  }

  /**
   * Return true if this invariant is necessarily true from a fact
   * that can be determined dynamically (after checking data).  Since
   * this method is dynamic, it should only be called after all
   * processing.
   *
   * <p> This method is final because subclasses should extend
   * isObviousDynamically(VarInfo[]) since that method is more general.
   **/
  public final DiscardInfo isObviousDynamically() {
    Assert.assertTrue (!Daikon.isInferencing);
    return isObviousDynamically (ppt.var_infos);
  }

  /**
   * Return true if this invariant and some equality combinations of
   * its member variables are dynamically obvious.  For example, a ==
   * b, and f(a) is obvious, so is f(b).  We use the someInEquality
   * (or least interesting) method during printing so we only print an
   * invariant if all its variables are interesting, since a single,
   * dynamic, non interesting occurance means all the equality
   * combinations aren't interesting.
   * @return the VarInfo array that contains the VarInfos that showed
   * this invariant to be obvious.  The contains variables that are
   * elementwise in the same equality set as this.ppt.var_infos.  Can
   * be null if no such assignment exists.
   **/
  public DiscardInfo isObviousDynamically_SomeInEquality() {
    DiscardInfo result = isObviousDynamically();
    if (result != null)
      return result;
    return isObviousDynamically_SomeInEqualityHelper (this.ppt.var_infos,
                                                     new VarInfo[this.ppt.var_infos.length],
                                                     0);
  }

  /**
   * Recurse through vis (an array of leaders) and generate the cartesian
   * product of their equality sets; in other words, every combination of
   * one element from each equality set.  For each such combination, test
   * isObviousDynamically; if any test is true, then return that
   * combination.  The combinations are generated via recursive calls to
   * this routine.
   **/
  protected DiscardInfo isObviousDynamically_SomeInEqualityHelper(VarInfo[] vis,
                                                             VarInfo[] assigned,
                                                             int position) {
    if (position == vis.length) {
      // base case
      if (debugIsObvious.isLoggable(Level.FINE)) {
        StringBuffer sb = new StringBuffer();
        sb.append ("  isObviousDynamically_SomeInEquality: ");
        for (int i = 0; i < vis.length; i++) {
          sb.append (assigned[i].name() + " ");
        }
        debugIsObvious.fine (sb.toString());
      }
      return isObviousDynamically (assigned);
    } else {
      // recursive case
      for (VarInfo vi : vis[position].get_equalitySet_vars ()) {
        assigned[position] = vi;
        DiscardInfo temp =
          isObviousDynamically_SomeInEqualityHelper (vis, assigned, position + 1);
        if (temp != null) return temp;
      }
      return null;
    }
  }


  /**
   * @return true if this invariant is only over prestate variables .
   */
  public boolean isAllPrestate() {
    return ppt.allPrestate();
  }

  // The notion of "interesting" embodied by this method is
  // unclear. You'd probably be better off using
  // hasUninterestingConstant(), or some other filter.
  // Uninteresting invariants will override this method to return
  // false
  public boolean isInteresting() {
    return true;
  }


  /** This is the test that's planned to replace the poorly specified
   * "isInteresting" check. In the future, the set of interesting
   * constants might be determined by a static analysis of the source
   * code, but for the moment, we only consider -1, 0, 1, and 2 as
   * interesting.
   *
   * Intuitively, the justification for this test is that an invariant
   * that includes an uninteresting constant (say, "size(x[]) < 237")
   * is likely to be an artifact of the way the program was tested,
   * rather than a statement that would in fact hold over all possible
   * executions. */
  public boolean hasUninterestingConstant() {
    return false;
  }

  // Orders invariants by class, then by variable names.  If the
  // invariants are both of class Implication, they are ordered by
  // comparing the predicate, then the consequent.
  public static final class ClassVarnameComparator implements Comparator<Invariant> {
    public int compare(Invariant inv1, Invariant inv2) {

      if (inv1 instanceof Implication && inv2 instanceof Implication)
        return compareImplications((Implication) inv1, (Implication) inv2);

      int compareClass = compareClass(inv1, inv2);
      if (compareClass != 0)
        return compareClass;

      return compareVariables(inv1, inv2);
    }

    // Returns 0 if the invariants are of the same class.  Else,
    // returns the comparison of the class names.
    private int compareClass(Invariant inv1, Invariant inv2) {
      if (inv1.getClass().equals(inv2.getClass())) {
        if (inv1 instanceof DummyInvariant) {
          // This special case is needed because the other code
          // assumes that all invariants of a given class have the
          // same arity.
          String df1 = inv1.format();
          String df2 = inv2.format();
          int cmp = df1.compareTo(df2);
          if (cmp != 0)
            return cmp;
          return inv1.ppt.var_infos.length - inv2.ppt.var_infos.length;
        }
        return 0;
      } else {
        String classname1 = inv1.getClass().getName();
        String classname2 = inv2.getClass().getName();
        return classname1.compareTo(classname2);
      }
    }

    // Returns 0 if the invariants have the same variable names.
    // Else, returns the comparison of the first variable names that
    // differ.  Requires that the invariants be of the same class.
    private int compareVariables(Invariant inv1, Invariant inv2) {
      VarInfo[] vars1 = inv1.ppt.var_infos;
      VarInfo[] vars2 = inv2.ppt.var_infos;

      // due to inv type match already
      assert vars1.length == vars2.length :
        "Bad type match: " + inv1.format() + " vs. " + inv2.format();

      for (int i=0; i < vars1.length; i++) {
        VarInfo var1 = vars1[i];
        VarInfo var2 = vars2[i];
        int compare = var1.name().compareTo(var2.name());
        if (compare != 0)
          return compare;
      }

      // All the variable names matched
      return 0;
    }

    private int compareImplications(Implication inv1, Implication inv2) {
      int comparePredicate = compare(inv1.predicate(), inv2.predicate());
      if (comparePredicate != 0)
        return comparePredicate;

      return compare(inv1.consequent(), inv2.consequent());
    }
  }

  /**
   * Orders invariants by class, then variable names, then formula.
   * If the formulas are the same, compares the printed representation
   * obtained from the format() method.
   **/
  public static final class ClassVarnameFormulaComparator
    implements Comparator<Invariant> {

    Comparator<Invariant> classVarnameComparator = new ClassVarnameComparator();

    public int compare(Invariant inv1, Invariant inv2) {
      int compareClassVarname = classVarnameComparator.compare(inv1, inv2);

      if (compareClassVarname != 0) {
        return compareClassVarname;
      }

      if (inv1.isSameInvariant(inv2)) {
        return 0;
      }

      int result = inv1.format().compareTo(inv2.format());

      // The purpose of the assertion below would seem to be to insist that
      // anything that doesn't return true to isSameInvariant() will not
      // produce the same written formula.  This can happen, however, if the
      // variables have a different order (as in function binary), but the
      // swapped variables are actually the same (since we create invariants
      // of the form f(a, a, a) because of equality sets.
      // Assert.assertTrue(result != 0
      //                   , "isSameInvariant() returned false "
      //                   + "(isSameFormula returned " + inv1.isSameFormula(inv2) + ")," + lineSep
      //                   + "but format().compareTo() returned 0:" + lineSep
      //                   + "  " + inv1.format() + lineSep + "      "  + inv1.repr() + lineSep
      //                   + "    " + inv1.ppt.parent.name + lineSep
      //                   + "  " + inv2.format() + lineSep + "      "  + inv2.repr() + lineSep
      //                   + "    " + inv1.ppt.parent.name + lineSep
      //                  );

      return result;
    }
  }

  /**
   * Class used as a key to store invariants in a MAP where their
   * equality depends on the invariant representing the same invariant
   * (i.e., their class is the same) and the same internal state (when
   * multiple invariants with the same class are possible)
   *
   * Note that this is based on the Invariant type (i.e., class) and the
   * internal state and not on what ppt the invariant is in or what
   * variables it is over.  Thus, invariants from different ppts are
   * the same if they represent the same type of invariant.
   */
  public static class Match {

    public Invariant inv;

    public Match (Invariant inv) {
      this.inv = inv;
    }

    public boolean equals (Object obj) {
      if (!(obj instanceof Match))
        return (false);

      Match ic = (Match) obj;
      return (ic.inv.match (inv));
    }

    public int hashCode() {
      return (inv.getClass().hashCode());
    }
  }

  /**
   * Returns whether or not two invariants are of the same type.  To
   * be of the same type, invariants must be of the same class.
   * Some invariant classes represent multiple invariants (such as
   * FunctionBinary).  They must also by the same formula.
   * Note that invariants with different formulas based on their
   * samples (LinearBinary, Bounds, etc) will still match as
   * long as the mergeFormulaOk() method returns true
   */

  public boolean match (Invariant inv) {

    if (inv.getClass() ==  getClass())
      return (inv.mergeFormulasOk() || isSameFormula (inv));
    else
      return (false);
  }

  /**
   * Returns whether or not the invariant matches the specified state.
   * Must be overriden by subclasses that support this.  Otherwise, it
   * returns true only if the state is null.
   */
  public boolean state_match (Object state) {
    return (state == null);
  }

  /**
   * Create a guarding predicate for a given invariant.
   * Returns null if no guarding is needed.
   **/
  public Invariant createGuardingPredicate(boolean install) {
    if (debugGuarding.isLoggable(Level.FINE)) {
      debugGuarding.fine ("Guarding predicate being created for: ");
      debugGuarding.fine ("  " + this.format());
    }

    // Find which VarInfos must be guarded
    List<VarInfo> mustBeGuarded = getGuardingList();

    if (mustBeGuarded.isEmpty()) {
      if (debugGuarding.isLoggable(Level.FINE)) {
        debugGuarding.fine ("No guarding is needed, returning");
      }
      return null;
    }

    // This conjunction would look better if it was built up right-to-left.
    Invariant guardingPredicate = null;
    for (VarInfo vi : mustBeGuarded) {
      Invariant currentGuard = vi.createGuardingPredicate(install);
      if (currentGuard == null)
        continue;
      debugGuarding.fine (String.format("VarInfo %s guard is %s", vi, currentGuard));
      if (guardingPredicate == null) {
        guardingPredicate = currentGuard;
      } else {
        guardingPredicate = new AndJoiner(ppt.parent, guardingPredicate, currentGuard);
      }
      debugGuarding.fine (String.format("  predicate so far: %s", guardingPredicate));
    }

    // If the guarding predicate has been previously constructed, return it.
    // Otherwise, we will return the newly constructed one.
    // This algorithm is inefficient.
    if (mustBeGuarded.size() > 1) {
      Invariants joinerViewInvs = ppt.parent.joiner_view.invs;
      for (Invariant currentInv : joinerViewInvs) {
        if (currentInv.isSameInvariant(guardingPredicate)) {
          return currentInv;
        }
      }
    }
    return guardingPredicate;
  }

  // Gets a list of all the variables that must be guarded for this
  // invariant.
  public List<VarInfo> getGuardingList() {
    return getGuardingList(ppt.var_infos);
  }

  public static List<VarInfo> getGuardingList(VarInfo[] varInfos) {
    List<VarInfo> guardingList = new ArrayList<VarInfo>();

    for (int i=0; i<varInfos.length; i++) {
      // debugGuarding.fine (varInfos[i]);
      guardingList.addAll(varInfos[i].getGuardingList());
      // debugGuarding.fine (guardingSet.toString());
    }

    return UtilMDE.removeDuplicates(guardingList);
  }


  /**
   * This procedure guards one invariant and returns the resulting guarded
   * invariant (implication), without placing it in any slice and without
   * modifying the original invariant.
   * Returns null if the invariant does not need to be guarded.
   **/
  public Invariant createGuardedInvariant(boolean install) {
    if (Daikon.dkconfig_guardNulls == "never") { // interned
      return null;
    }

    if (debugGuarding.isLoggable(Level.FINE)) {
      debugGuarding.fine ("  Trying to add guard for: " + this + "; repr = " + repr());
    }
    if (isGuardingPredicate) {
      debugGuarding.fine ("  Do not guard: this is a guarding predicate");
      return null;
    }
    Invariant guardingPredicate = createGuardingPredicate(install);
    if (debugGuarding.isLoggable(Level.FINE)) {
      if (guardingPredicate != null) {
        debugGuarding.fine ("  Predicate: " +
                            guardingPredicate.format());
        debugGuarding.fine ("  Consequent: " +
                            format());
      } else {
        debugGuarding.fine ("  No implication needed");
      }
    }

    if (guardingPredicate == null) {
      return null;
    }

    Implication guardingImplication =
      GuardingImplication.makeGuardingImplication(ppt.parent, guardingPredicate, this, false);

    if (install) {
      if (! ppt.parent.joiner_view.hasImplication(guardingImplication)) {
        ppt.parent.joiner_view.addInvariant(guardingImplication);
      }
    }
    return guardingImplication;
  }


  /**
   * Instantiates an invariant of the same class on the specified
   * slice.  Must be overridden in each class.  Must be used rather
   * than clone so that checks in instantiate for reasonable invariants
   * are done.
   * @return the new invariant
   */
  protected Invariant instantiate_dyn (PptSlice slice) {
    Assert.assertTrue (false, "no instantiate_dyn for class " + getClass());
    return (null);
  }

  /**
   * Returns whether or not this class of invariants are currently
   * enabled
   */
  public boolean enabled() {
    Assert.assertTrue (false, "no enabled for class " + getClass());
    return (false);
  }


  /**
   * Returns whether or not the invariant is valid over the specified
   * types.
   */
  // public boolean valid_types (ProglangType[] rep_types) {
  //  Assert.assertTrue (false, "no valid_types for class " + getClass());
  //  return (false);
  // }

  /**
   * Returns whether or not the invariant is valid over the basic types
   * in vis.  This only checks basic types (scalar, string, array, etc)
   * and should match the basic superclasses of invariant (SingleFloat,
   * SingleScalarSequence, ThreeScalar, etc).  More complex checks
   * that depend on variable details can be implemented in instantiate_ok()
   *
   * @see #instantiate_ok(VarInfo[])
   */
  public boolean valid_types (VarInfo[] vis) {
    Assert.assertTrue (false, "no valid_types for class " + getClass());
    return (false);
  }

  /**
   * Checks to see if the invariant can reasonably be instantiated over
   * the specified variables.  Checks details beyond what is provided
   * by valid_types.  This should never be called without calling
   * valid_types first (implementations should be able to presume that
   * valid_types is true).
   *
   * @see #valid_types(VarInfo[])
   */
  public boolean instantiate_ok (VarInfo[] vis) {
    return (true);
  }

  /**
   * Instantiates this invariant over the specified slice.  The slice
   * must not be null and its variables must be valid for this type of
   * invariant.  Returns null if the invariant is not enabled or if the
   * invariant is not reasonable over the specified variables.  Otherwise
   * returns the new invariant
   */
  public Invariant instantiate (PptSlice slice) {

    assert slice != null;
    if (! valid_types(slice.var_infos)) {
      System.out.printf("this.getClass(): %s%n", this.getClass());
      System.out.printf("slice: %s%n", slice);
      System.out.printf("slice.var_infos: %s%n", (Object)slice.var_infos);
      System.out.printf("ppt: %s%n", ppt);
      // Can't do this, as this might be a "prototype" invariant.
      // System.out.printf("this: %s%n", this.repr());
    }
    assert valid_types(slice.var_infos)
      : String.format("valid_types(%s) = false for %s", slice.var_infos, this);
    if (!enabled() || !instantiate_ok (slice.var_infos))
      return (null);
    Invariant inv = instantiate_dyn (slice);
    Assert.assertTrue (inv != null);
    if (inv.ppt == null) {
      // Save creating the message if the check succeeds
      Assert.assertTrue (inv.ppt != null, "invariant class " + inv.getClass());
    }
    return (inv);
  }

  /**
   * Adds the specified sample to the invariant and returns the result.
   */
  public InvariantStatus add_sample (ValueTuple vt, int count) {

    if (ppt instanceof PptSlice1) {

      VarInfo v = ppt.var_infos[0];
      UnaryInvariant unary_inv = (UnaryInvariant) this;
      return (unary_inv.add (vt.getValue(v), vt.getModified(v), count));

    } else if (ppt instanceof PptSlice2) {

      VarInfo v1 = ppt.var_infos[0];
      VarInfo v2 = ppt.var_infos[1];
      BinaryInvariant bin_inv = (BinaryInvariant) this;
      return (bin_inv.add_unordered (vt.getValue(v1), vt.getValue(v2),
                                      vt.getModified(v1), count));

    } else /* must be ternary */ {

      VarInfo v1 = ppt.var_infos[0];
      VarInfo v2 = ppt.var_infos[1];
      VarInfo v3 = ppt.var_infos[2];
      if (!(this instanceof TernaryInvariant))
        Assert.assertTrue (false, "invariant '" + format() + "' in slice "
                           + ppt.name() + " is not ternary");
      TernaryInvariant ternary_inv = (TernaryInvariant) this;
      return (ternary_inv.add (vt.getValue(v1), vt.getValue(v2),
                                vt.getValue(v3), vt.getModified(v1), count));
    }
  }

  /**
   * Check the rep invariants of this.
   **/
  public void repCheck() {
  }

  /**
   * Returns whether or not the invariant is currently active.  This is
   * used to identify those invariants that require a certain number
   * of points before they actually do computation (eg, LinearBinary)
   *
   * This is used during suppresion.  Any invariant that is not active
   * cannot suppress another invariant
   */
  public boolean isActive() {
    return (true);
  }

  // TODO: The logDetail and (especially) logOn methods are misleading,
  // because they are static but are very often called with an instance as
  // the receiver, suggesting that they have something to do with the
  // receiver.  This should be corrected.  -MDE

  /**
   * Returns whether or not detailed logging is on.  Note that this check
   * is not performed inside the logging calls themselves, it must be
   * performed by the caller.
   *
   * @see daikon.Debug#logDetail()
   * @see daikon.Debug#logOn()
   * @see daikon.Debug#log(Logger, Class, Ppt, String)
   */

  public static boolean logDetail () {
    return (Debug.logDetail());
  }

  /**
   * Returns whether or not logging is on.
   *
   * @see daikon.Debug#logOn()
   */

  public static boolean logOn() {
    return (Debug.logOn());
  }

  /**
   * Logs a description of the invariant and the specified msg via the
   * logger as described in {@link daikon.Debug#log(Logger, Class, Ppt,
   * VarInfo[], String)}.
   */

  public void log (Logger log, String msg) {

    if (Debug.logOn()) {
      Debug.log (log, getClass(), ppt, msg);
    }
  }


 /**
  * Logs a description of the invariant and the specified msg via the
  * logger as described in {@link daikon.Debug#log(Logger, Class, Ppt,
  * VarInfo[], String)}.
  *
  * @return whether or not it logged anything
  */

  public boolean log (String format, Object...args) {
    if (ppt != null) {
      String msg = format;
      if (args.length > 0)
        msg = String.format (format, args);
      return (Debug.log (getClass(), ppt, msg));
    } else
      return (false);
  }

  public String toString() {
    return format();
  }

  public static String toString (Invariant[] invs) {

    ArrayList<String> strings = new ArrayList<String>(invs.length);
    for (int i = 0; i < invs.length; i++) {
      if (invs[i] == null)
        strings.add("null");
      else
        strings.add(invs[i].format());
    }
    return UtilMDE.join(strings, ", ");
  }


  /**
   *  Used throught Java family formatting of invariants.
   *
   *  Returns
   *
   *     "utilMDE.FuzzyFloat.method(v1_name, v2_name)"
   *
   *  Where v1_name and v2_name are the properly formatted
   *  varinfos v1 and v2, under the given format.
   *
   *  Author: Carlos Pacheco
   */
  // [[ This method doesn't belong here. But where? ]]
  public static String formatFuzzy(String method,
                                    VarInfo v1,
                                    VarInfo v2,
                                    OutputFormat format) {

    StringBuffer results = new StringBuffer();
    return
      results
      .append("daikon.Quant.fuzzy.")
      .append(method)
      .append("(")
      .append(v1.name_using(format))
      .append(", ")
      .append(v2.name_using(format))
      .append(")")
      .toString();
  }


}



//     def format(self, args=None):
//         if self.one_of:
//             # If it can be None, print it only if it is always None and
//             # is an invariant over non-derived variable.
//             if self.can_be_None:
//                 if ((len(self.one_of) == 1)
//                     and self.var_infos):
//                     some_nonderived = false
//                     for vi in self.var_infos:
//                      some_nonderived = some_nonderived or not vi.is_derived
//                     if some_nonderived:
//                         return "%s = uninit" % (args,)
//             elif len(self.one_of) == 1:
//                 return "%s = %s" % (args, self.one_of[0])
//             ## Perhaps I should unconditionally return this value;
//             ## otherwise I end up printing ranges more often than small
//             ## numbers of values (because when few values and many samples,
//             ## the range always looks justified).
//             # If few samples, don't try to infer a function over the values;
//             # just return the list.
//             elif (len(self.one_of) <= 3) or (self.samples < 100):
//                 return "%s in %s" % (args, util.format_as_set(self.one_of))
//         return None
