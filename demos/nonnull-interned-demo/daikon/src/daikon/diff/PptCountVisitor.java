package daikon.diff;

import daikon.inv.Invariant;
import daikon.inv.OutputFormat;
import java.io.*;
import daikon.*;
import java.util.*;

/**
 * PptCountVisitor is currently not documented.
 *
 * @author Lee Lin
 **/
public class PptCountVisitor extends PrintAllVisitor {


  // amount of invariants needed for a program point to be
  // flagged as suspicious
  private static final int REPORT_REQUIREMENT_NUMBER = 1;
  private static final int GOAL_REQUIREMENT_NUMBER = 1;

  // invariants found by the splitting
  private HashSet<String> cnt = new HashSet<String>();
  // target set of invariants
  private HashSet<String> targSet = new HashSet<String>();
  // invariants found matching
  private HashSet<String> correctSet = new HashSet<String>();

  // invariants reported but not correct
  private HashSet<String> incorrectSet = new HashSet<String>();



  private HashMap<String,HashSet<String>> goodMap = new HashMap<String,HashSet<String>>();



  public PptCountVisitor (PrintStream ps, boolean verbose,
                          boolean printEmptyPpts) {
    super(ps, verbose, printEmptyPpts);
  }

  // throw out Program points that are Conditional,
  public void visit (PptNode node) {
    PptTopLevel ppt = node.getPpt1();
    if ((ppt instanceof PptConditional)) return;
    //        else super.visit (node);

    boolean report = countReport (node);
    boolean target = countTarget (node);

    if (report) {
      cnt.add (ppt.name());
    }

    if (target) {
      targSet.add (ppt.name());
    }

    if (report && target) {
      correctSet.add (ppt.name());
    }
  }

  private boolean countReport (PptNode input) {

    int reportCnt = 0;
    int totalCnt = 0;

    for (Iterator<InvNode> i = input.children(); i.hasNext(); ) {
      InvNode node = i.next();
      Invariant inv1 = node.getInv1();
      Invariant inv2 = node.getInv2();

      totalCnt++;

      if (inv1 != null && inv1.justified() && !filterOut (inv1)) {
        reportCnt ++;
      }

    }

    return reportCnt > REPORT_REQUIREMENT_NUMBER;


  }

  private boolean countTarget (PptNode input) {

    int targetCnt = 0;
    int totalCnt = 0;

    for (Iterator<InvNode> i = input.children(); i.hasNext(); ) {
      InvNode node = i.next();
      Invariant inv1 = node.getInv1();
      Invariant inv2 = node.getInv2();

      totalCnt++;

      if (inv2 != null && inv2.justified() && !filterOut (inv2)) {
        targetCnt ++;
      }

    }

    return targetCnt > GOAL_REQUIREMENT_NUMBER;
  }


  /** Anytime something matches, we should score it has correct */
  public void visit(InvNode node) {
    Invariant inv1 = node.getInv1();
    Invariant inv2 = node.getInv2();

    String key1 = "";
    String key2 = "";

    if (inv1 != null && inv1.justified() && !filterOut (inv1)) {
      String thisPptName1 = inv1.ppt.name();

      key1 = thisPptName1 + "$" + inv1.format();
      cnt.add (key1);
    }

    if (inv2 != null && inv2.justified() && !filterOut (inv2)) {
      String thisPptName2 = inv2.ppt.name();
      key2 = thisPptName2 + "$" + inv2.format();
      targSet.add (key2);
    }

    if (shouldPrint(inv1, inv2)) {
      // inv1 and inv2 should be the same, so it doesn't matter
      // which one we choose when adding to recall -LL
      correctSet.add (key2);

      //	System.out.println("K1: " + key1);
      //        System.out.println ("K2: " + key2);

      String thisPptName1 = inv1.ppt.name();
      // System.out.println ("NAME1: " + tmpStr1);
      // Contest.smallestRoom(II)I:::EXIT;condition="not(max <= num)"
      String bucketKey = thisPptName1.substring (0,
                                                 thisPptName1.lastIndexOf (";condition"));


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
  protected static boolean shouldPrint(Invariant inv1, Invariant inv2) {

    if (5 == 5) {
      if (inv1 == null || inv2 == null) {
        return false;
      }
      return  inv1.format().equals (inv2.format());
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

    if (inv == null) return true;
    String str = inv.format_using(OutputFormat.JAVA);
    StringTokenizer st = new StringTokenizer (str, " ()");
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

  public double calcPrecision() {

    System.out.println ("Prec: " + correctSet.size() + " / " + cnt.size());
    if (cnt.size() == 0) return -1; // to avoid a divide by zero -LL
    return (double) correctSet.size() / cnt.size();
  }


  /** Prints the results of the correct set in a human-readable format */
  public void printFinal () {

    System.out.println ("CORRECT_FOUND: ");
    for (String str : targSet) {
      if (correctSet.contains (str)) {
        System.out.println (str);
      }
    }

    System.out.println ();
    System.out.println ();
    System.out.println ();
    System.out.println ("NOT_FOUND: ");
    for (String str : targSet) {
      if (!correctSet.contains (str)) {
        System.out.println (str);
      }
    }

    System.out.println ();
    System.out.println ();
    System.out.println ();
    System.out.println ("WRONG_REPORTS: ");
    for (String str : incorrectSet) {
      System.out.println (str);
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
