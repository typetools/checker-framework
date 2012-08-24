package utilMDE;

import java.util.*;

/**
 * Routines for doing 'fuzzy' floating point comparisons.  Those are
 * comparisons that only require the floating point numbers to be
 * relatively close to one another to be equal, rather than exactly
 * equal. <p>
 *
 * Floating point numbers are compared for equality by dividing them by
 * one another and comparing the ratio.  By default they must be within
 * 0.0001 (0.01%) to be considered equal; set this value with set_rel_diff.
 * Note that zero is never equal to a non-zero number using this method. <p>
 *
 * Additionally two NaN values are always considered equal. <p>
 **/

public class FuzzyFloat {

  /** Minimum ratio between two floats that will act as equal. */
  double min_ratio = 0.9999;
  /** Maximum ratio between two floats that will act as equal. */
  double max_ratio = 1.0001;

  /** True if ratio test turned off. */
  boolean off = false;

  public FuzzyFloat () {
  }

  /**
   * Specify the specific relative difference allowed between two
   * floats in order for them to be equal.  The default is 0.0001
   * a relative diff of zero, disables it (i.e., only exact matches work).
   */
  public FuzzyFloat (double rel_diff) {
    set_rel_diff (rel_diff);
  }

  /**
   * Set the relative diff after creation.
   *
   * @see #FuzzyFloat
   */
  public void set_rel_diff (double rel_diff) {
    min_ratio = 1 - rel_diff;
    max_ratio = 1 + rel_diff;
    off = (rel_diff == 0.0);
    //System.out.println ("min_ratio = " + min_ratio + ", max_ratio = "
    //                    + max_ratio);

  }

  /**
   * Test d1 and d2 for equality using the current ratio.  Two NaN floats
   * are not considered equal (consistent with the == operator). <p>
   *
   * Note that if one of the numbers if 0.0, then the other number must
   * be less than the square of the fuzzy ratio.  This policy accomodates
   * round off errors in floating point values.
   *
   * @return true if d1 and d2 are considered equal, false otherwise
   */

  /* @ pure */ public boolean eq (double d1, double d2) {

    // NaNs are not considered equal.
    if (Double.isNaN(d1) && Double.isNaN(d2))
      return (false);

    // if zero was specified for a ratio, don't do the divide.  You might
    // get slightly different answers.  And this should be faster.
    if (off)
      return (d1 == d2);

    // slightly more efficient for matches and catches positive and negative
    // infinity (which match in this test, but not below)
    if (d1 == d2)
      return (true);

    // when one number is 0, check that the other is less than the square
    // of the fuzzy ration (accomodates round off errors in floating point
    // values)

    if (d1 == 0.0 || d2 == 0.0) {

      double zero_tolerance = Math.pow((max_ratio - 1), 2);

      if (d1 == 0.0) {

        return (Math.abs(d2) < zero_tolerance);

      } else {

        return (Math.abs(d1) < zero_tolerance);
      }
    }

    double ratio = d1/d2;
    return ((ratio >= min_ratio) && (ratio <= max_ratio));
  }

  /**
   * Test d1 and d2 for non-equality using the current ratio.
   *
   * @see #eq
   */
  /* @ pure */ public boolean ne (double d1, double d2) {
   return (!eq (d1, d2));
  }

  /**
   * Test d1 and d2 for d1 < d2.  If d1 is equal to d2 using the current ratio
   * this returns false.
   *
   * @see #eq
   */
  /* @ pure */ public boolean lt (double d1, double d2) {
    return ((d1 < d2) && ne (d1, d2));
  }

  /**
   * test d1 and  d2 for d1 <= d2.  If d1 is equal to d2 using the current
   * ratio, this returns true.
   *
   * @see #eq
   */
  /* @ pure */ public boolean lte (double d1, double d2) {
    return ((d1 <= d2) || eq (d1, d2));
  }

  /**
   * test d1 and d2  for d1 > d2.  IF d1 is equal to d2 using the current
   * ratio, this returns false.
   *
   * @see #eq
   */
  /* @ pure */ public boolean gt (double d1, double d2) {
      return ((d1 > d2) && ne (d1, d2));
  }

  /**
   * test d1 and  d2 for d1 >= d2.  If d1 is equal to d2 using the current
   * ratio, this returns true.
   *
   * @see #eq
   */
  /* @ pure */ public boolean gte (double d1, double d2) {
    return ((d1 >= d2) || eq (d1, d2));
  }

  /**
   * Searches for the first occurrence of elt in a.  elt is considered
   * equal to a[i] if it passes the {@link #eq} test.
   *
   * @return the first index containing the specified element,
   *    or -1 if the element is not found in the array.
   * @see java.util.Vector#indexOf(java.lang.Object)
   **/
  /* @ pure */ public int indexOf (double[] a, double elt) {
     for (int i=0; i<a.length; i++)
       if (eq (elt, a[i]))
        return i;
    return -1;
  }

  /**
   * Searches for the first subsequence of a that matches sub elementwise.
   * Elements of sub are considered to match elements of a if they pass
   * the {@link #eq} test.
   *
   * @return the first index whose subarray is equal to the specified array
   *    or -1 if no such subarray is found in the array.
   * @see java.util.Vector#indexOf(java.lang.Object)
   * @see java.lang.String#indexOf(java.lang.String)
   **/
  /* @ pure */ public int indexOf (double[] a, double[] sub) {

    int a_index_max = a.length - sub.length;

    outer: for (int i = 0; i <= a_index_max; i++) {
      for (int j = 0; j < sub.length; j++) {
        if (ne (a[i+j], sub[j])) {
          continue outer;
        }
      }
      return (i);
    }
    return (-1);
  }

  /**
   * Determines whether or not a1 and a2 are set equivalent (contain only the
   * same elements).  Element comparison uses {@link #eq}. <p>
   *
   * Note that this implementation is optimized for cases where the
   * elements are actually the same, since it does a sort of both arrays
   * before starting the comparisons.
   *
   * @return true if a1 and a2 are set equivalent, false otherwise
   */
  /* @ pure */ public boolean isElemMatch (double[] a1, double[] a2) {

    //don't change our parameters
    a1 = a1.clone();
    a2 = a2.clone();

    Arrays.sort (a1);
    Arrays.sort (a2);

    // look for elements of a2 in a1
    int start = 0;
    outer1: for (int i = 0; i < a2.length; i++) {
      double val = a2[i];
      for (int j = start; j < a1.length; j++) {
        if (eq (val, a1[j])) {
          start = j;
          continue outer1;
        }
        if (val < a1[j]) {
          // System.out.println ("isElemMatch: " + val + " " + a1[j]);
          return (false);
        }
      }
      // System.out.println ("isElemMatch: " + i);
      return (false);
    }

    // look for elements of a1 in a2
    start = 0;
    outer2: for (int i = 0; i < a1.length; i++) {
      double val = a1[i];
      for (int j = start; j < a2.length; j++) {
        if (eq (val, a2[j])) {
          start = j;
          continue outer2;
        }
        if (val < a2[j]) {
          // System.out.println ("isElemMatch: " + val + " " + a2[j]);
          return (false);
        }
      }
      // System.out.println ("isElemMatch: " + i);
      return (false);
    }

    return (true);
  }

    // Slightly more efficient method that will miss some matches
//     int i = 0;
//     int j = 0;
//     while (i < a1.length && j < a2.length) {
//       if (ne (a1[i], a2[j])) {
//         System.out.println ("isElemMatch: " + a1[i] + " " + a2[j]);
//         return (false);
//       }
//       double val = a1[i];
//       i++;
//       while ((i < a1.length) && (eq (a1[i], val))) {
//         i++;
//       }
//       j++;
//       while ((j < a2.length) && (eq (a2[j], val))) {
//         j++;
//       }
//     }

//     // if there are any elements left, then they don't match.
//     if ((i != a1.length) || (j != a2.length)) {
//       System.out.println ("isElemMatch: " + i + " " + j);
//       return (false);
//     }

//     return (true);
//     }



  /**
   * Lexically compares two double arrays.
   */

  /* @ pure */ public class DoubleArrayComparatorLexical implements Comparator<double[]> {

    /**
     * Lexically compares o1 and o2 as double arrays.
     *
     * @return positive if o1 > 02, 0 if 01 == 02, negative if 01 < 02
     */
    public int compare(double[] a1, double[] a2) {
      if (a1 == a2)
        return 0;
      int len = Math.min(a1.length, a2.length);
      for (int i=0; i<len; i++) {
        if (ne (a1[i], a2[i])) {
          return ((a1[i] > a2[i]) ? 1 : -1);
        }
      }
      return a1.length - a2.length;
    }
  }

  /**
   * Determines whether smaller is a subset of bigger.  Element
   * comparison uses {@link #eq}. <p>
   *
   * Note that this implementation is optimized for cases where the
   * elements are actually the same, since it does a sort of both
   * arrays before starting the comparisons.
   *
   * @return true if smaller is a subset (each element of smaller is
   * also a element of bigger) of bigger, false otherwise
   */

  /* @ pure */ public boolean isSubset (double[] smaller, double[] bigger) {

    //don't change our parameters
    smaller = smaller.clone();
    bigger = bigger.clone();

    Arrays.sort (smaller);
    Arrays.sort (bigger);

    // look for elements of smaller in bigger
    int start = 0;
    outer1: for (int i = 0; i < smaller.length; i++) {
      double val = smaller[i];
      for (int j = start; j < bigger.length; j++) {
        if (eq (val, bigger[j])) {
          start = j;
          continue outer1;
        }
        if (val < bigger[j]) {
          return (false);
        }
      }
      return (false);
    }

    return (true);
  }

}
