package daikon.diff;

import daikon.inv.Invariant;
import daikon.inv.OutputFormat;
import java.io.*;
import daikon.*;
import java.util.*;

/**
 * MatchCountVisitor is a visitor that almost does the opposite of
 * PrintDifferingInvariantsVisitor.  MatchCount prints invariant pairs
 * if they are the same, and only if they are a part of a conditional ppt.
 * The visitor also accumulates some state during its traversal for statistics,
 * and can report the match precision.
 *
 *
 * @author Lee Lin
 **/
public class MatchCountVisitor2 extends PrintAllVisitor {


  // invariants found by the splitting
  private HashSet<String> cnt = new HashSet<String>();
  // target set of invariants
  private HashSet<String> targSet = new HashSet<String>();
  // invariants found matching
  private HashSet<String> correctSet = new HashSet<String>();

  // invariants reported but not correct
  private HashSet<String> incorrectSet = new HashSet<String>();



  private HashMap<String,HashSet<String>> goodMap = new HashMap<String,HashSet<String>>();




  public MatchCountVisitor2 (PrintStream ps, boolean verbose,
                             boolean printEmptyPpts) {
    super(ps, verbose, printEmptyPpts);
  }

  // throw out Program points that are Conditional,
  public void visit (PptNode node) {
    PptTopLevel ppt = node.getPpt1();
    if ((ppt instanceof PptConditional)) return;
    else super.visit (node);
  }


  /** Anytime a consequent matches a target, we should score it as correct */
  public void visit(InvNode node) {
    Invariant inv1 = node.getInv1();
    Invariant inv2 = node.getInv2();

    if (inv1 != null && !(inv1.ppt.parent instanceof PptConditional)) { return; }

    String key1 = "";
    // String key2 = "";

    if (inv1 != null && inv1.justified() && !filterOut (inv1)) {
      String thisPptName1 = inv1.ppt.name();

      key1 = thisPptName1 + "$" + inv1.format();
      //        cnt.add (key1);
      cnt.add (inv1.format());
    }

    if (inv2 != null /*&& inv2.justified()*/ && !filterOut (inv2)) {
      String thisPptName2 = inv2.ppt.name();

      // Looks like implications work on EXIT points, do they work
      // on ENTER points?
      // key2 = thisPptName2 + "$" + inv2.format();
      // if (key2.indexOf ("ENTER") == -1)
      //   targSet.add (key2);

      // Don't allow implications in goal, as suggested by
      // Iuliu
      if (! (inv2 instanceof daikon.inv.Implication))
        targSet.add (inv2.format());
    }

    if (shouldPrint(inv1, inv2)) {
      // inv1 and inv2 should be the same, so it doesn't matter
      // which one we choose when adding to recall -LL
      correctSet.add (key1);

      //	System.out.println("K1: " + key1);
      //        System.out.println ("K2: " + key2);

      String thisPptName1 = inv1.ppt.name();
      // System.out.println ("NAME1: " + tmpStr1);
      // Contest.smallestRoom(II)I:::EXIT;condition="not(max <= num)"
      String bucketKey =  thisPptName1.indexOf (";condition") > -1 ?

        thisPptName1.substring (0,
                                thisPptName1.lastIndexOf (";condition"))
        : thisPptName1;


      /** this is all for printing purposes */

      String predicate = extractPredicate (thisPptName1);
      HashSet<String> bucket = goodMap.get (bucketKey);
      if (bucket == null) {
        bucket = new HashSet<String>();
        goodMap.put (bucketKey, bucket);
      }
      bucket.add (predicate + " ==> " + inv1.format());

    }

    else {
      incorrectSet.add (key1);
    }
  }

  /** grabs the splitting condition from a pptname */
  private String extractPredicate (String s) {
    int cut = s.indexOf (";condition=");
    if (cut == -1) return "NO_PREDICATE: ";
    return s.substring (cut + 12, s.lastIndexOf('"'));
  }


  /** s is a program point name that looks like "blah blah:::EXIT107(arg1, arg2)"
   *  find the point just after the EXIT107 */
  private int findCutoff (String s) {
    String lastPart = "";
    int cut = 0;
    if (s.indexOf ("EXIT") > -1) {
      cut = s.indexOf ("EXIT");
      lastPart = s.substring (cut);

    }

    else if (s.indexOf ("ENTER") > -1) {
      cut = s.indexOf ("ENTER");
      lastPart = s.substring (cut);
    }

    else {
      System.out.println ("Should not get here, PPT name not ENTER/EXIT");
    }

    return cut + lastPart.indexOf("(");

  }

  /** Returns true if the pair of invariants should be printed **/
  protected boolean shouldPrint(Invariant inv1, Invariant inv2) {

    if (5 == 5) {
      if (inv1 == null || inv2 == null) {
        return false;
      }
      return  inv1.format().equals (inv2.format()) ||
        targSet.contains (inv1.format());
    }

    int rel = DetailedStatisticsVisitor.determineRelationship(inv1, inv2);
    if (rel == DetailedStatisticsVisitor.REL_SAME_JUST1_JUST2 ) {

      // got rid of unjustified
      //   rel == DetailedStatisticsVisitor.REL_SAME_UNJUST1_UNJUST2)

      // Added to get rid of constants other than -1, 0, 1 in the
      // invariant's format_java() string... this change was made to
      // filter out targets that could never really be achived
      // example:   num >= 10378

      if (filterOut (inv1) || filterOut (inv2)) {
        return false;
      }

      // now you have a match

      return true;
    }


    return false;
  }

  /** returns true iff any token of inv.format_java() contains
   *  a number other than -1, 0, 1 or is null. */
  private static boolean filterOut (Invariant inv) {

    if (5 == 5) return false;


    if (inv == null) return true;
    String str = inv.format_using(OutputFormat.JAVA);
    StringTokenizer st = new StringTokenizer (str, " ()],[");
    while (st.hasMoreTokens()) {
      String oneToken = st.nextToken();
      try {
        char firstChar = oneToken.charAt(0);
        // remember identifiers can not begin with [0-9\-]
        if (Character.isDigit (firstChar) || firstChar == '-') {
          if (acceptableNumber (oneToken)) {
            continue;
          }
          else return true;
        }

      }
      catch (NumberFormatException e) {
        System.out.println ("Should never get here... " +
                            "NumberFormatException in filterOut: " +
                            oneToken);
        continue;
      }
    }
    return false;
  }

  public double calcRecall() {
    System.out.println ("Recall: " + correctSet.size() + " / " + targSet.size());
    if (targSet.size() == 0) return -1; // avoids divide by zero
    return (double) correctSet.size() / targSet.size();
  }


  /** returns true iff numLiteral represents a numeric
   * literal string of integer or float that we believe
   * will be useful for a splitting condition.  Usually that
   * includes -1, 0, 1, and any other numeric literal
   * found in the source code.  */
  private static boolean acceptableNumber (String numLiteral) {

    // need to make sure that it is an integer vs. floating
    // point number

    // could be float, look for "."
    if (numLiteral.indexOf (".") > -1) {
      float fnum = Float.parseFloat (numLiteral);
      if (fnum == 1.0 || fnum == 0.0 || fnum == -1.0) {
        return true;
      }

      return false;
    }
    // not float, must be int
    else {
      int num = Integer.parseInt (numLiteral);

      // accept -1, 0, 1
      if (num == -1 || num == 0 || num == 1)
        return true;
      else return false;
    }

  }

  private void finish() {
    correctSet.clear();
    for (String elem : cnt) {
      if (targSet.contains (elem))
        correctSet.add (elem);
    }
  }

  public double calcPrecision() {
    finish();
    System.out.println ("Prec: " + correctSet.size() + " / " + cnt.size());
    if (cnt.size() == 0) return -1; // to avoid a divide by zero -LL
    return (double) correctSet.size() / cnt.size();
  }


  /** Prints the results of the correct set in a human-readable format */
  public void printFinal () {
    finish();
    System.out.println ("CORRECT_FOUND: ");
    for (String str : targSet) {
      if (correctSet.contains (str)) {
        System.out.println (str);
      }
    }

    System.out.println ();
    System.out.println ();
    System.out.println ();
    System.out.println ("NOT FOUND: ");
    for (String str : targSet) {
      if (!correctSet.contains (str)) {
        System.out.println (str);
      }
    }

    System.out.println ();
    System.out.println ();
    System.out.println ();
    System.out.println ("WRONG_REPORTS: ");
    //        for (Iterator i = incorrectSet.iterator(); i.hasNext(); ) {
    for (String str : cnt) {
      if (!correctSet.contains (str)) {
        System.out.println (str);
      }
    }



    for (String ppt : goodMap.keySet()) {
      System.out.println ();
      System.out.println ("*****************" + ppt);
      for (String s : goodMap.get(ppt)) {
        System.out.println (s);
      }
    }


  }

}
