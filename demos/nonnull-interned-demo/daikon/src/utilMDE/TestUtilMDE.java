package utilMDE;

import junit.framework.*;
import java.util.*;
import java.io.*;
import java.util.regex.*;
import static utilMDE.Options.ArgException;

// run like this:
//   java utilMDE.TestUtilMDE

// Files to test:
// ArraysMDE.java
// Assert.java
// CountingPrintWriter.java
// Digest.java
// FuzzyFloat.java
// Hasher.java
// Intern.java
// LimitedSizeIntSet.java
// MathMDE.java
// Options.java
// OrderedPairIterator.java
// TestUtilMDE.java
// StringBuilderDelimited.java
// UtilMDE.java
// WeakHasherMap.java

/** Test code for the utilMDE package. */
public final class TestUtilMDE extends TestCase {

  public static void main(String[] args) {
    junit.textui.TestRunner.run(new TestSuite(TestUtilMDE.class));
  }

  public TestUtilMDE(String name) {
    super(name);
  }

//   public static void main(String[] args) {
//     testTestUtilMDE();
//     testArraysMDE();
//     testHasher();
//     testIntern();
//     testMathMDE();
//     testOrderedPairIterator();
//     testUtilMDE();
//     testWeakHasherMap();
//     System.out.println("All utilMDE tests succeeded.");
//   }

// // I don't use Assert.assertTrue() because that can be turned off by setting
// // Assert.enabled = false, and I want these tests to always be performed.
//   private static final void assert(boolean b) { if (!b) throw new Error(); }
//   private static final void assert(boolean b, String s) { if (!b) throw new Error(s); }
  public static final void assert_arrays_equals(int[] a1, int[] a2) {
     boolean result = Arrays.equals(a1, a2);
     if (! result)
       System.out.println("Arrays differ: " + ArraysMDE.toString(a1)
                          + ", " + ArraysMDE.toString(a2));
     assertTrue(result);
//      assert(Arrays.equals(a1, a2),
//         "Arrays differ: " + ArraysMDE.toString(a1) + ", " + ArraysMDE.toString(a2));
   }
  public static final void assert_arrays_equals(double[] a1, double[] a2) {
     boolean result = Arrays.equals(a1, a2);
     if (! result)
       System.out.println("Arrays differ: " + ArraysMDE.toString(a1)
                          + ", " + ArraysMDE.toString(a2));
     assertTrue(result);
   }


  ///////////////////////////////////////////////////////////////////////////
  /// Utility functions
  ///

  public static Iterator<Integer> int_array_iterator(int[] nums) {
    Integer[] o = new Integer[nums.length];
    for (int i=0; i<nums.length; i++)
      o[i] = new Integer(nums[i]);
    return Arrays.asList(o).iterator();
  }

  public static int[] int_iterator_array(Iterator<Integer> itor) {
    Vector<Integer> v = new Vector<Integer>();
    while (itor.hasNext())
      v.add(itor.next());
    int[] a = new int[v.size()];
    for (int i=0; i<a.length; i++)
      a[i] = v.elementAt(i).intValue();
    return a;
  }

  public static <T> Vector<T> toVector(Iterator<T> itor) {
    Vector<T> v = new Vector<T>();
    for ( ; itor.hasNext() ; ) {
      v.add(itor.next());
    }
    return v;
  }

  public static <T> Vector<T> toVector(Enumeration<T> e) {
    Vector<T> v = new Vector<T>();
    for ( ; e.hasMoreElements() ; ) {
      v.add(e.nextElement());
    }
    return v;
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Now the actual testing
  ///

  public static void testArraysMDE() {

    // public static int min(int[] a)
    assertTrue(ArraysMDE.min(new int[] { 1,2,3 }) == 1);
    assertTrue(ArraysMDE.min(new int[] { 2,33,1 }) == 1);
    assertTrue(ArraysMDE.min(new int[] { 3,-2,1 }) == -2);
    assertTrue(ArraysMDE.min(new int[] { 3 }) == 3);

    // public static int max(int[] a)
    assertTrue(ArraysMDE.max(new int[] { 1,2,3 }) == 3);
    assertTrue(ArraysMDE.max(new int[] { 2,33,1 }) == 33);
    assertTrue(ArraysMDE.max(new int[] { 3,-2,1 }) == 3);
    assertTrue(ArraysMDE.max(new int[] { 3 }) == 3);

    // public static int[] min_max(int[] a)
    assert_arrays_equals(ArraysMDE.min_max(new int[] { 1,2,3 }),
                         new int[] { 1,3 });
    assert_arrays_equals(ArraysMDE.min_max(new int[] { 2,33,1 }),
                         new int[] { 1,33 });
    assert_arrays_equals(ArraysMDE.min_max(new int[] { 3,-2,1 }),
                         new int[] { -2,3 });
    assert_arrays_equals(ArraysMDE.min_max(new int[] { 3 }),
                         new int[] { 3,3 });

    // public static int sum(int[] a)
    assertTrue(0 == ArraysMDE.sum(new int[0]));
    assertTrue(10 == ArraysMDE.sum(new int[] {10}));
    assertTrue(10 == ArraysMDE.sum(new int[] {1, 2, 3, 4}));

    // public static int sum(int[][] a)
    assertTrue(0 == ArraysMDE.sum(new int[0][0]));
    assertTrue(78  == ArraysMDE.sum
           (new int[][] {{1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10, 11, 12}}));

    // public static double sum(double[] a)
    assertTrue(0 == ArraysMDE.sum(new double[0]));
    assertTrue(3.14 == ArraysMDE.sum(new double[] {3.14}));
    assertTrue(8.624 == ArraysMDE.sum(new double[] {3.14, 2.718, -1.234, 4}));

    // public static double sum(double[][] a)
    assertTrue(0 == ArraysMDE.sum(new double[0][0]));
    assertTrue(79.5  == ArraysMDE.sum(new double[][] {{1.1, 2.2, 3.3, 4.4},
                                                      {5.5, 6, 7, 8},
                                                      {9, 10, 11, 12}}));

    // public static int element_range(int[] a)
    assertTrue(ArraysMDE.element_range(new int[] { 1,2,3 }) == 2);
    assertTrue(ArraysMDE.element_range(new int[] { 2,33,1 }) == 32);
    assertTrue(ArraysMDE.element_range(new int[] { 3,-2,1 }) == 5);
    assertTrue(ArraysMDE.element_range(new int[] { 3 }) == 0);

    // public static int indexOf(Object[] a, Object elt)
    // public static int indexOfEq(Object[] a, Object elt)
    {
      Integer[] a = new Integer[10];
      for (int i=0; i<a.length; i++)
        a[i] = new Integer(i);
      assertTrue(ArraysMDE.indexOf(a, new Integer(-1)) == -1);
      assertTrue(ArraysMDE.indexOf(a, new Integer(0)) == 0);
      assertTrue(ArraysMDE.indexOf(a, new Integer(7)) == 7);
      assertTrue(ArraysMDE.indexOf(a, new Integer(9)) == 9);
      assertTrue(ArraysMDE.indexOf(a, new Integer(10)) == -1);
      assertTrue(ArraysMDE.indexOf(a, new Integer(20)) == -1);

      assertTrue(ArraysMDE.indexOfEq(a, new Integer(-1)) == -1);
      assertTrue(ArraysMDE.indexOfEq(a, new Integer(0)) == -1);
      assertTrue(ArraysMDE.indexOfEq(a, new Integer(7)) == -1);
      assertTrue(ArraysMDE.indexOfEq(a, new Integer(9)) == -1);
      assertTrue(ArraysMDE.indexOfEq(a, new Integer(10)) == -1);
      assertTrue(ArraysMDE.indexOfEq(a, new Integer(20)) == -1);
      assertTrue(ArraysMDE.indexOfEq(a, a[0]) == 0);
      assertTrue(ArraysMDE.indexOfEq(a, a[7]) == 7);
      assertTrue(ArraysMDE.indexOfEq(a, a[9]) == 9);
    }

    // public static int indexOf(int[] a, int elt)
    {
      int[] a = new int[10];
      for (int i=0; i<a.length; i++)
        a[i] = i;
      assertTrue(ArraysMDE.indexOf(a, -1) == -1);
      assertTrue(ArraysMDE.indexOf(a, 0) == 0);
      assertTrue(ArraysMDE.indexOf(a, 7) == 7);
      assertTrue(ArraysMDE.indexOf(a, 9) == 9);
      assertTrue(ArraysMDE.indexOf(a, 10) == -1);
      assertTrue(ArraysMDE.indexOf(a, 20) == -1);
    }

    // public static int indexOf(boolean[] a, boolean elt)
    {
      boolean[] a = new boolean[10];
      for (int i=0; i<a.length; i++)
        a[i] = false;
      assertTrue(ArraysMDE.indexOf(a, true) == -1);
      assertTrue(ArraysMDE.indexOf(a, false) == 0);
      a[9] = true;
      assertTrue(ArraysMDE.indexOf(a, true) == 9);
      assertTrue(ArraysMDE.indexOf(a, false) == 0);
      a[7] = true;
      assertTrue(ArraysMDE.indexOf(a, true) == 7);
      assertTrue(ArraysMDE.indexOf(a, false) == 0);
      a[0] = true;
      assertTrue(ArraysMDE.indexOf(a, true) == 0);
      assertTrue(ArraysMDE.indexOf(a, false) == 1);
      for (int i=0; i<a.length; i++)
        a[i] = true;
      assertTrue(ArraysMDE.indexOf(a, true) == 0);
      assertTrue(ArraysMDE.indexOf(a, false) == -1);
    }

    // public static int indexOf(Object[] a, Object[] sub)
    // public static int indexOfEq(Object[] a, Object[] sub)
    {
      Integer[] a = new Integer[10];
      for (int i=0; i<a.length; i++)
        a[i] = new Integer(i);
      Integer[] b = new Integer[] { };
      Integer[] c = new Integer[] { a[0], a[1], a[2] };
      Integer[] d = new Integer[] { a[1], a[2] };
      Integer[] e = new Integer[] { a[2], a[3], a[4], a[5] };
      Integer[] f = new Integer[] { a[7], a[8], a[9] };
      Integer[] g = new Integer[] { a[7], new Integer(8), a[9] };
      Integer[] h = new Integer[] { a[7], a[8], a[9], new Integer(10) };
      Integer[] c2 = new Integer[] { new Integer(0), new Integer(1), new Integer(2) };
      Integer[] d2 = new Integer[] { new Integer(1), new Integer(2) };
      Integer[] e2 = new Integer[] { new Integer(2), new Integer(3), new Integer(4), new Integer(5) };
      Integer[] f2 = new Integer[] { new Integer(7), new Integer(8), new Integer(9) };

      assertTrue(ArraysMDE.indexOf(a, b) == 0);
      assertTrue(ArraysMDE.indexOfEq(a, b) == 0);
      assertTrue(ArraysMDE.indexOf(a, c) == 0);
      assertTrue(ArraysMDE.indexOfEq(a, c) == 0);
      assertTrue(ArraysMDE.indexOf(a, c2) == 0);
      assertTrue(ArraysMDE.indexOfEq(a, c2) == -1);
      assertTrue(ArraysMDE.indexOf(a, d) == 1);
      assertTrue(ArraysMDE.indexOfEq(a, d) == 1);
      assertTrue(ArraysMDE.indexOf(a, d2) == 1);
      assertTrue(ArraysMDE.indexOfEq(a, d2) == -1);
      assertTrue(ArraysMDE.indexOf(a, e) == 2);
      assertTrue(ArraysMDE.indexOfEq(a, e) == 2);
      assertTrue(ArraysMDE.indexOf(a, e2) == 2);
      assertTrue(ArraysMDE.indexOfEq(a, e2) == -1);
      assertTrue(ArraysMDE.indexOf(a, f) == 7);
      assertTrue(ArraysMDE.indexOfEq(a, f) == 7);
      assertTrue(ArraysMDE.indexOf(a, f2) == 7);
      assertTrue(ArraysMDE.indexOfEq(a, f2) == -1);
      assertTrue(ArraysMDE.indexOf(a, g) == 7);
      assertTrue(ArraysMDE.indexOfEq(a, g) == -1);
      assertTrue(ArraysMDE.indexOf(a, h) == -1);
      assertTrue(ArraysMDE.indexOfEq(a, h) == -1);
    }


    // public static int indexOf(int[] a, int[] sub)
    {
      int[] a = new int[10];
      for (int i=0; i<a.length; i++)
        a[i] = i;
      int[] b = new int[] { };
      int[] c = new int[] { a[0], a[1], a[2] };
      int[] d = new int[] { a[1], a[2] };
      int[] e = new int[] { a[2], a[3], a[4], a[5] };
      int[] f = new int[] { a[7], a[8], a[9] };
      int[] g = new int[] { a[7], 22, a[9] };
      int[] h = new int[] { a[7], a[8], a[9], 10 };

      assertTrue(ArraysMDE.indexOf(a, b) == 0);
      assertTrue(ArraysMDE.indexOf(a, c) == 0);
      assertTrue(ArraysMDE.indexOf(a, d) == 1);
      assertTrue(ArraysMDE.indexOf(a, e) == 2);
      assertTrue(ArraysMDE.indexOf(a, f) == 7);
      assertTrue(ArraysMDE.indexOf(a, g) == -1);
      assertTrue(ArraysMDE.indexOf(a, h) == -1);

      // Tests pulled from actual StackAr data
      int[] origTheArray = new int[] {1267757, 1267757, 1267757, 1267757, 1267757, 1267757, 1267757, 1267757, 1267757, 1267757, 1267757, 0, 0, 0, 0, 0, 0, 0, 0, 0};

      int[] postTheArray = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
      assertTrue(ArraysMDE.indexOf(postTheArray, origTheArray) == -1);
      assertTrue(ArraysMDE.indexOf(origTheArray, postTheArray) == -1);

    }

    // public static int indexOf(boolean[] a, boolean[] sub)
    // [I'm punting on this for now; deal with it later...]

    // public static Object[] subarray(Object[] a, int startindex, int length)
    // public static byte[] subarray(byte[] a, int startindex, int length)
    // public static boolean[] subarray(boolean[] a, int startindex, int length)
    // public static char[] subarray(char[] a, int startindex, int length)
    // public static double[] subarray(double[] a, int startindex, int length)
    // public static float[] subarray(float[] a, int startindex, int length)
    // public static int[] subarray(int[] a, int startindex, int length)
    // public static long[] subarray(long[] a, int startindex, int length)
    // public static short[] subarray(short[] a, int startindex, int length)

    // public static boolean isSubarray(Object[] a, Object[] sub, int a_offset)
    // public static boolean isSubarrayEq(Object[] a, Object[] sub, int a_offset)
    // public static boolean isSubarray(int[] a, int[] sub, int a_offset)
    // public static boolean isSubarray(boolean[] a, boolean[] sub, int a_offset)
    // (The subarray tests are missing; I hope that the array indexOf
    // operations above test them sufficiently.)

    // static String toString(int[] a)
    assertTrue(ArraysMDE.toString(new int[] { }).equals("[]"));
    assertTrue(ArraysMDE.toString(new int[] { 0 }).equals("[0]"));
    assertTrue(ArraysMDE.toString(new int[] { 0,1,2 }).equals("[0, 1, 2]"));

    // public static boolean sorted(int[] a)
    assertTrue(ArraysMDE.sorted(new int[] { 0,1,2 }));
    assertTrue(ArraysMDE.sorted(new int[] { 0,1,2,2,3,3 }));
    assertTrue(ArraysMDE.sorted(new int[] { }));
    assertTrue(ArraysMDE.sorted(new int[] { 0 }));
    assertTrue(ArraysMDE.sorted(new int[] { 0,1 }));
    assertTrue(!ArraysMDE.sorted(new int[] { 1,0 }));
    assertTrue(!ArraysMDE.sorted(new int[] { 0,1,2,1,2,3 }));

    // public static int noDuplicates(int[] a)
    assertTrue (ArraysMDE.noDuplicates(new int[] {1, 2, 3, 5, 4,0}) == true);
    assertTrue (ArraysMDE.noDuplicates(new int[] {1, 2, 3, 5, 4,100}) == true);
    assertTrue (ArraysMDE.noDuplicates(new int[] {2, 2, 3, 5, 4,0}) == false);
    assertTrue (ArraysMDE.noDuplicates(new int[] {1, 2, 3, 5, 4,1}) == false);
    assertTrue (ArraysMDE.noDuplicates(new int[] {1, 2, -3, -5, 4,0}) == true);
    assertTrue (ArraysMDE.noDuplicates(new int[] {1, 2, -2, -2, 4,100}) == false);
    assertTrue (ArraysMDE.noDuplicates(new int[] {}) == true);
    assertTrue (ArraysMDE.noDuplicates(new int[] {42}) == true);

    // public static int noDuplicates(long[] a)
    assertTrue (ArraysMDE.noDuplicates(new long[] {1, 2, 3, 5, 4,0}) == true);
    assertTrue (ArraysMDE.noDuplicates(new long[] {1, 2, 3, 5, 4,100}) == true);
    assertTrue (ArraysMDE.noDuplicates(new long[] {2, 2, 3, 5, 4,0}) == false);
    assertTrue (ArraysMDE.noDuplicates(new long[] {1, 2, 3, 5, 4,1}) == false);
    assertTrue (ArraysMDE.noDuplicates(new long[] {1, 2, -3, -5, 4,0}) == true);
    assertTrue (ArraysMDE.noDuplicates(new long[] {1, 2, -2, -2, 4,100}) == false);
    assertTrue (ArraysMDE.noDuplicates(new long[] {}) == true);
    assertTrue (ArraysMDE.noDuplicates(new long[] {42}) == true);

    // public static int noDuplicates(double[] a)
    assertTrue (ArraysMDE.noDuplicates(new double[] {1, 2, 3, 5, 4,0}) == true);
    assertTrue (ArraysMDE.noDuplicates(new double[] {1, 2, 3, 5, 4,100}) == true);
    assertTrue (ArraysMDE.noDuplicates(new double[] {2, 2, 3, 5, 4,0}) == false);
    assertTrue (ArraysMDE.noDuplicates(new double[] {1, 2, 3, 5, 4,1}) == false);
    assertTrue (ArraysMDE.noDuplicates(new double[] {1., 1.001, -3, -5, 4,0}) == true);
    assertTrue (ArraysMDE.noDuplicates(new double[] {1., 2, -2.00, -2, 4,100}) == false);
    assertTrue (ArraysMDE.noDuplicates(new double[] {}) == true);
    assertTrue (ArraysMDE.noDuplicates(new double[] {42}) == true);

    // public static int noDuplicates(String[] a)
    assertTrue (ArraysMDE.noDuplicates(new String[] {"1", "2", "3", "5", "4","0"})
                == true);
    assertTrue (ArraysMDE.noDuplicates(new String[] {"A","a", "foo", "Foo",""})
                == true);
    assertTrue (ArraysMDE.noDuplicates(new String[] {" ", " "})
                == false);
    assertTrue (ArraysMDE.noDuplicates(new String[] {"  ", " "})
                == true);

    // public static boolean fn_is_permutation(int[] a)
    assertTrue(ArraysMDE.fn_is_permutation(new int[] { 0, 1, 2, 3 }) == true);
    assertTrue(ArraysMDE.fn_is_permutation(new int[] { 1, 2, 3, 0 }) == true);
    assertTrue(ArraysMDE.fn_is_permutation(new int[] { 3, 2, 1, 0 }) == true);
    assertTrue(ArraysMDE.fn_is_permutation(new int[] { 0, 1, 2, 2 }) == false);
    assertTrue(ArraysMDE.fn_is_permutation(new int[] { 0, -1, 2, 3 }) == false);
    assertTrue(ArraysMDE.fn_is_permutation(new int[] { 0, 1, 2, 4 }) == false);
    assertTrue(ArraysMDE.fn_is_permutation(new int[] { 0, 0, 0, 0 }) == false);

    // public static boolean fn_is_total(int[] a)
    assertTrue(ArraysMDE.fn_is_total(new int[] { 0, 1, 2, 3 }) == true);
    assertTrue(ArraysMDE.fn_is_total(new int[] { 1, 2, 3, 0 }) == true);
    assertTrue(ArraysMDE.fn_is_total(new int[] { 3, 2, 1, 0 }) == true);
    assertTrue(ArraysMDE.fn_is_total(new int[] { 0, 1, 2, 2 }) == true);
    assertTrue(ArraysMDE.fn_is_total(new int[] { -1, 0, 2, 3 }) == false);
    assertTrue(ArraysMDE.fn_is_total(new int[] { 0, -1, 2, 3 }) == false);
    assertTrue(ArraysMDE.fn_is_total(new int[] { 0, -2, 1, 3 }) == true); // weird
    assertTrue(ArraysMDE.fn_is_total(new int[] { 0, 2, 3, -1 }) == false);
    assertTrue(ArraysMDE.fn_is_total(new int[] { 0, 1, 2, 4 }) == true);
    assertTrue(ArraysMDE.fn_is_total(new int[] { 0, 0, 0, 0 }) == true);

    // public static int[] fn_identity(int length)
    assert_arrays_equals(ArraysMDE.fn_identity(0), new int[] { });
    assert_arrays_equals(ArraysMDE.fn_identity(1), new int[] { 0 });
    assert_arrays_equals(ArraysMDE.fn_identity(2), new int[] { 0, 1 });
    assert_arrays_equals(ArraysMDE.fn_identity(3), new int[] { 0, 1, 2 });

    // public static int[] fn_inverse_permutation(int[] a)
    assert_arrays_equals(ArraysMDE.fn_inverse_permutation(
                            new int[] { 0, 1, 2, 3 }),
                            new int[] { 0, 1, 2, 3 });
    assert_arrays_equals(ArraysMDE.fn_inverse_permutation(
                            new int[] { 1, 2, 3, 0 }),
                            new int[] { 3, 0, 1, 2 });
    assert_arrays_equals(ArraysMDE.fn_inverse_permutation(
                            new int[] { 3, 2, 1, 0 }),
                            new int[] { 3, 2, 1, 0 });

    // public static int[] fn_inverse(int[] a, int arange)
    assert_arrays_equals(ArraysMDE.fn_inverse(
                            new int[] { 0, 1, 2, 3 }, 4),
                            new int[] { 0, 1, 2, 3 });
    assert_arrays_equals(ArraysMDE.fn_inverse(
                            new int[] { 1, 2, 3, 0 }, 4),
                            new int[] { 3, 0, 1, 2 });
    assert_arrays_equals(ArraysMDE.fn_inverse(
                            new int[] { 3, 2, 1, 0 }, 4),
                            new int[] { 3, 2, 1, 0 });
    try {
      ArraysMDE.fn_inverse(new int[] { 0, 0, 2, 3 }, 4);
      throw new Error();
    } catch (UnsupportedOperationException e) {
      assertTrue(e.getMessage().equals("Not invertible"));
    }
    assert_arrays_equals(ArraysMDE.fn_inverse(
                            new int[] { 5 }, 6),
                            new int[] { -1, -1, -1, -1, -1, 0 });
    assert_arrays_equals(ArraysMDE.fn_inverse(
                            new int[] { 1, 2, 3, 5 }, 6),
                            new int[] { -1, 0, 1, 2, -1, 3 });


    // public static int[] fn_compose(int[] a, int[] b)
    assert_arrays_equals(ArraysMDE.fn_compose(
                            new int[] { 0, 1, 2, 3 },
                            new int[] { 0, 1, 2, 3 }),
                            new int[] { 0, 1, 2, 3 });
    assert_arrays_equals(ArraysMDE.fn_compose(
                            new int[] { 1, 2, 3, 0 },
                            new int[] { 1, 2, 3, 0 }),
                            new int[] { 2, 3, 0, 1 });
    assert_arrays_equals(ArraysMDE.fn_compose(
                            new int[] { 3, 2, 1, 0 },
                            new int[] { 3, 2, 1, 0 }),
                            new int[] { 0, 1, 2, 3 });
    assert_arrays_equals(ArraysMDE.fn_compose(
                            new int[] { 0, 1, 0, 3 },
                            new int[] { 0, 5, 2, 1 }),
                            new int[] { 0, 5, 0, 1 });
    assert_arrays_equals(ArraysMDE.fn_compose(
                            new int[] { 0 },
                            new int[] { 5 }),
                            new int[] { 5 });
    assert_arrays_equals(ArraysMDE.fn_compose(
                            new int[] { 1, 2, 3, 5 },
                            new int[] { 1, 2, 3, 5, -1, -1 }),
                            new int[] { 2, 3, 5, -1 });

    // public static boolean isSubset(long[] smaller, long[] bigger)
    // public static boolean isSubset(double[] smaller, double[] bigger)
    // public static boolean isSubset(String[] smaller, String[] bigger)

    {
      double[] f1 = new double[10];
      double[] f2 = new double[20];

      for (int j = 0; j < f2.length; j++)
        f2[j] = j;
      for (int i = 0; i < f2.length - f1.length; i++) {

        //fill up f1 with elements of f2
        for (int j = 0; j < f1.length; j++)
          f1[j] = f2[i+j];

        f1[5] = f2[i];

        double[] f1_copy = f1.clone();
        double[] f2_copy = f2.clone();

        assertTrue (ArraysMDE.isSubset (f1, f2));
        assert_arrays_equals (f1, f1_copy);
        assert_arrays_equals (f2, f2_copy);
      }

      double [] a1 = new double [] {1, 5, 10};
      double [] a2 = new double [] {};
      double [] a3 = new double [] {1};
      double [] a4 = new double [] {10};
      double [] a5 = new double [] {1, 10, 15, 20};
      double [] a6 = new double [] {10, 10, 10, 10, 10, 1};

      assertTrue (ArraysMDE.isSubset (a2, a1));
      assertTrue (!ArraysMDE.isSubset (a1, a2));
      assertTrue (!ArraysMDE.isSubset (a1, a5));
      assertTrue (ArraysMDE.isSubset (a3, a1));
      assertTrue (ArraysMDE.isSubset (a4, a1));
      assertTrue (ArraysMDE.isSubset (a6, a1));
      assertTrue (!ArraysMDE.isSubset (a1, a6));
    }

    // public static class IntArrayComparatorLexical implements Comparator
    // public static class IntArrayComparatorLengthFirst implements Comparator
    {
      Comparator<int[]> iacl = new ArraysMDE.IntArrayComparatorLexical();
      Comparator<int[]> iaclf = new ArraysMDE.IntArrayComparatorLengthFirst();

      int[] a0 = new int[] { };
      int[] a1 = new int[] { };
      int[] a2 = new int[] { 0,1,2,3 };
      int[] a3 = new int[] { 0,1,2,3,0 };
      int[] a4 = new int[] { 0,1,2,3,4 };
      int[] a5 = new int[] { 0,1,2,3,4 };
      int[] a6 = new int[] { 0,1,5,3,4 };
      int[] a7 = new int[] { 1,2,3,4 };
      int[] a8 = new int[] { -5 };
      int[] a9 = new int[] { Integer.MAX_VALUE };
      int[] a10= new int[] { Integer.MIN_VALUE };

      assertTrue(iacl.compare(a0, a1) == 0);
      assertTrue(iaclf.compare(a0, a1) == 0);
      assertTrue(iacl.compare(a1, a0) == 0);
      assertTrue(iaclf.compare(a1, a0) == 0);
      assertTrue(iacl.compare(a1, a2) < 0);
      assertTrue(iaclf.compare(a1, a2) < 0);
      assertTrue(iacl.compare(a2, a1) > 0);
      assertTrue(iaclf.compare(a2, a1) > 0);
      assertTrue(iacl.compare(a2, a3) < 0);
      assertTrue(iaclf.compare(a2, a3) < 0);
      assertTrue(iacl.compare(a3, a2) > 0);
      assertTrue(iaclf.compare(a3, a2) > 0);
      assertTrue(iacl.compare(a3, a4) < 0);
      assertTrue(iaclf.compare(a3, a4) < 0);
      assertTrue(iacl.compare(a4, a3) > 0);
      assertTrue(iaclf.compare(a4, a3) > 0);
      assertTrue(iacl.compare(a4, a5) == 0);
      assertTrue(iaclf.compare(a4, a5) == 0);
      assertTrue(iacl.compare(a5, a4) == 0);
      assertTrue(iaclf.compare(a5, a4) == 0);
      assertTrue(iacl.compare(a5, a6) < 0);
      assertTrue(iaclf.compare(a5, a6) < 0);
      assertTrue(iacl.compare(a6, a5) > 0);
      assertTrue(iaclf.compare(a6, a5) > 0);
      assertTrue(iacl.compare(a6, a7) < 0);
      assertTrue(iaclf.compare(a6, a7) > 0);
      assertTrue(iacl.compare(a7, a6) > 0);
      assertTrue(iaclf.compare(a7, a6) < 0);
      assertTrue(iacl.compare(a1, a4) < 0);
      assertTrue(iaclf.compare(a1, a4) < 0);
      assertTrue(iacl.compare(a4, a1) > 0);
      assertTrue(iaclf.compare(a4, a1) > 0);
      assertTrue(iacl.compare(a2, a4) < 0);
      assertTrue(iaclf.compare(a2, a4) < 0);
      assertTrue(iacl.compare(a4, a2) > 0);
      assertTrue(iaclf.compare(a4, a2) > 0);
      assertTrue(iacl.compare(a6, a4) > 0);
      assertTrue(iaclf.compare(a6, a4) > 0);
      assertTrue(iacl.compare(a4, a6) < 0);
      assertTrue(iaclf.compare(a4, a6) < 0);
      assertTrue(iacl.compare(a7, a4) > 0);
      assertTrue(iaclf.compare(a7, a4) < 0);
      assertTrue(iacl.compare(a4, a7) < 0);
      assertTrue(iaclf.compare(a4, a7) > 0);
      assertTrue(iacl.compare (a8, a9) < 0);
      assertTrue(iaclf.compare (a8, a9) < 0);
      assertTrue(iacl.compare(a10, a7) < 0);
    }

    // public static class LongArrayComparatorLexical implements Comparator
    // public static class LongArrayComparatorLengthFirst implements Comparator
    {
      Comparator<long[]> lacl = new ArraysMDE.LongArrayComparatorLexical();
      Comparator<long[]> laclf = new ArraysMDE.LongArrayComparatorLengthFirst();
      long[] a0 = new long[] { };
      long[] a1 = new long[] { };
      long[] a2 = new long[] { 0,1,2,3 };
      long[] a3 = new long[] { 0,1,2,3,0 };
      long[] a4 = new long[] { 0,1,2,3,4 };
      long[] a5 = new long[] { 0,1,2,3,4 };
      long[] a6 = new long[] { 0,1,5,3,4 };
      long[] a7 = new long[] { 1,2,3,4 };
      long[] a8 = new long[] { -5 };
      long[] a9 = new long[] { Long.MAX_VALUE };
      long[] a10= new long[] { Long.MIN_VALUE };

      assertTrue(lacl.compare(a0, a1) == 0);
      assertTrue(laclf.compare(a0, a1) == 0);
      assertTrue(lacl.compare(a1, a0) == 0);
      assertTrue(laclf.compare(a1, a0) == 0);
      assertTrue(lacl.compare(a1, a2) < 0);
      assertTrue(laclf.compare(a1, a2) < 0);
      assertTrue(lacl.compare(a2, a1) > 0);
      assertTrue(laclf.compare(a2, a1) > 0);
      assertTrue(lacl.compare(a2, a3) < 0);
      assertTrue(laclf.compare(a2, a3) < 0);
      assertTrue(lacl.compare(a3, a2) > 0);
      assertTrue(laclf.compare(a3, a2) > 0);
      assertTrue(lacl.compare(a3, a4) < 0);
      assertTrue(laclf.compare(a3, a4) < 0);
      assertTrue(lacl.compare(a4, a3) > 0);
      assertTrue(laclf.compare(a4, a3) > 0);
      assertTrue(lacl.compare(a4, a5) == 0);
      assertTrue(laclf.compare(a4, a5) == 0);
      assertTrue(lacl.compare(a5, a4) == 0);
      assertTrue(laclf.compare(a5, a4) == 0);
      assertTrue(lacl.compare(a5, a6) < 0);
      assertTrue(laclf.compare(a5, a6) < 0);
      assertTrue(lacl.compare(a6, a5) > 0);
      assertTrue(laclf.compare(a6, a5) > 0);
      assertTrue(lacl.compare(a6, a7) < 0);
      assertTrue(laclf.compare(a6, a7) > 0);
      assertTrue(lacl.compare(a7, a6) > 0);
      assertTrue(laclf.compare(a7, a6) < 0);
      assertTrue(lacl.compare(a1, a4) < 0);
      assertTrue(laclf.compare(a1, a4) < 0);
      assertTrue(lacl.compare(a4, a1) > 0);
      assertTrue(laclf.compare(a4, a1) > 0);
      assertTrue(lacl.compare(a2, a4) < 0);
      assertTrue(laclf.compare(a2, a4) < 0);
      assertTrue(lacl.compare(a4, a2) > 0);
      assertTrue(laclf.compare(a4, a2) > 0);
      assertTrue(lacl.compare(a6, a4) > 0);
      assertTrue(laclf.compare(a6, a4) > 0);
      assertTrue(lacl.compare(a4, a6) < 0);
      assertTrue(laclf.compare(a4, a6) < 0);
      assertTrue(lacl.compare(a7, a4) > 0);
      assertTrue(laclf.compare(a7, a4) < 0);
      assertTrue(lacl.compare(a4, a7) < 0);
      assertTrue(laclf.compare(a4, a7) > 0);
      assertTrue(lacl.compare (a8, a9) < 0);
      assertTrue(laclf.compare (a8, a9) < 0);
      assertTrue(lacl.compare(a10, a7) < 0);
    }

    // public static class DoubleArrayComparatorLexical implements Comparator
    {
      Comparator<double[]> dacl = new ArraysMDE.DoubleArrayComparatorLexical();
      double[] a0 = new double[] { };
      double[] a1 = new double[] { };
      double[] a2 = new double[] { 0,1,2,3 };
      double[] a3 = new double[] { 0,1,2,3,0 };
      double[] a4 = new double[] { 0,1,2,3,4 };
      double[] a5 = new double[] { 0,1,2,3,4 };
      double[] a6 = new double[] { 0,1,5,3,4 };
      double[] a7 = new double[] { 1,2,3,4 };
      double[] a8 = new double[] { 0.005 };
      double[] a9 = new double[] { 0.004 };
      double[] a10= new double[] { -0.005 };
      double[] a11= new double[] { -0.004 };
      double[] a12= new double[] { 10.0 * Integer.MAX_VALUE };
      double[] a13= new double[] { 10.0 * Integer.MIN_VALUE };

      assertTrue(dacl.compare(a0, a1) == 0);
      assertTrue(dacl.compare(a1, a0) == 0);
      assertTrue(dacl.compare(a1, a2) < 0);
      assertTrue(dacl.compare(a2, a1) > 0);
      assertTrue(dacl.compare(a2, a3) < 0);
      assertTrue(dacl.compare(a3, a2) > 0);
      assertTrue(dacl.compare(a3, a4) < 0);
      assertTrue(dacl.compare(a4, a3) > 0);
      assertTrue(dacl.compare(a4, a5) == 0);
      assertTrue(dacl.compare(a5, a4) == 0);
      assertTrue(dacl.compare(a5, a6) < 0);
      assertTrue(dacl.compare(a6, a5) > 0);
      assertTrue(dacl.compare(a6, a7) < 0);
      assertTrue(dacl.compare(a7, a6) > 0);
      assertTrue(dacl.compare(a1, a4) < 0);
      assertTrue(dacl.compare(a4, a1) > 0);
      assertTrue(dacl.compare(a2, a4) < 0);
      assertTrue(dacl.compare(a4, a2) > 0);
      assertTrue(dacl.compare(a6, a4) > 0);
      assertTrue(dacl.compare(a4, a6) < 0);
      assertTrue(dacl.compare(a7, a4) > 0);
      assertTrue(dacl.compare(a4, a7) < 0);

      // Test the comparisons on small/large numbers
      assertTrue(dacl.compare(a8, a9) > 0);
      assertTrue(dacl.compare(a10, a11) < 0);
      assertTrue(dacl.compare(a11, a12) < 0);
      assertTrue(dacl.compare(a12, a13) > 0);
      assertTrue(dacl.compare(a13, a11) < 0);
    }

    // public static class ObjectArrayComparatorLexical implements Comparator
    // public static class ObjectArrayComparatorLengthFirst implements Comparator

    // public static final class ComparableArrayComparatorLexical implements Comparator
    // public static final class ComparableArrayComparatorLengthFirst implements Comparator
{
      Comparator<String[]> cacl = new ArraysMDE.ComparableArrayComparatorLexical<String>();
      Comparator<String[]> caclf = new ArraysMDE.ComparableArrayComparatorLengthFirst<String>();
      String[] a0 = new String[] { };
      String[] a1 = new String[] { };
      String[] a2 = new String[] { "0","1","2","3" };
      String[] a3 = new String[] { "0","1","2","3","0" };
      String[] a4 = new String[] { "0","1","2","3","4" };
      String[] a5 = new String[] { "0","1","2","3","4" };
      String[] a6 = new String[] { "0","1","5","3","4" };
      String[] a7 = new String[] { "1","2","3","4" };
      String[] a8 = new String[] { "0","1",null,"3","4" };

      assertTrue(cacl.compare(a0, a1) == 0);
      assertTrue(caclf.compare(a0, a1) == 0);
      assertTrue(cacl.compare(a1, a0) == 0);
      assertTrue(caclf.compare(a1, a0) == 0);
      assertTrue(cacl.compare(a1, a2) < 0);
      assertTrue(caclf.compare(a1, a2) < 0);
      assertTrue(cacl.compare(a2, a1) > 0);
      assertTrue(caclf.compare(a2, a1) > 0);
      assertTrue(cacl.compare(a2, a3) < 0);
      assertTrue(caclf.compare(a2, a3) < 0);
      assertTrue(cacl.compare(a3, a2) > 0);
      assertTrue(caclf.compare(a3, a2) > 0);
      assertTrue(cacl.compare(a3, a4) < 0);
      assertTrue(caclf.compare(a3, a4) < 0);
      assertTrue(cacl.compare(a4, a3) > 0);
      assertTrue(caclf.compare(a4, a3) > 0);
      assertTrue(cacl.compare(a4, a5) == 0);
      assertTrue(caclf.compare(a4, a5) == 0);
      assertTrue(cacl.compare(a5, a4) == 0);
      assertTrue(caclf.compare(a5, a4) == 0);
      assertTrue(cacl.compare(a5, a6) < 0);
      assertTrue(caclf.compare(a5, a6) < 0);
      assertTrue(cacl.compare(a6, a5) > 0);
      assertTrue(caclf.compare(a6, a5) > 0);
      assertTrue(cacl.compare(a6, a7) < 0);
      assertTrue(caclf.compare(a6, a7) > 0);
      assertTrue(cacl.compare(a7, a6) > 0);
      assertTrue(caclf.compare(a7, a6) < 0);
      assertTrue(cacl.compare(a1, a4) < 0);
      assertTrue(caclf.compare(a1, a4) < 0);
      assertTrue(cacl.compare(a4, a1) > 0);
      assertTrue(caclf.compare(a4, a1) > 0);
      assertTrue(cacl.compare(a2, a4) < 0);
      assertTrue(caclf.compare(a2, a4) < 0);
      assertTrue(cacl.compare(a4, a2) > 0);
      assertTrue(caclf.compare(a4, a2) > 0);
      assertTrue(cacl.compare(a6, a4) > 0);
      assertTrue(caclf.compare(a6, a4) > 0);
      assertTrue(cacl.compare(a4, a6) < 0);
      assertTrue(caclf.compare(a4, a6) < 0);
      assertTrue(cacl.compare(a7, a4) > 0);
      assertTrue(caclf.compare(a7, a4) < 0);
      assertTrue(cacl.compare(a8, a1) > 0);
      assertTrue(caclf.compare(a8, a1) > 0);
      assertTrue(cacl.compare(a1, a8) < 0);
      assertTrue(caclf.compare(a1, a8) < 0);
      assertTrue(cacl.compare(a8, a2) < 0);
      assertTrue(caclf.compare(a8, a2) > 0);
      assertTrue(cacl.compare(a2, a8) > 0);
      assertTrue(caclf.compare(a2, a8) < 0);
      assertTrue(cacl.compare(a8, a3) < 0);
      assertTrue(caclf.compare(a8, a3) < 0);
      assertTrue(cacl.compare(a3, a8) > 0);
      assertTrue(caclf.compare(a3, a8) > 0);
    }

    // public static boolean any_null(Object[] a)
    {
      Object o = new Object();
      assertTrue(ArraysMDE.any_null(new Object[] { }) == false);
      assertTrue(ArraysMDE.any_null(new Object[] { null }) == true);
      assertTrue(ArraysMDE.any_null(new Object[] { null, null }) == true);
      assertTrue(ArraysMDE.any_null(new Object[] { o }) == false);
      assertTrue(ArraysMDE.any_null(new Object[] { o, o }) == false);
      assertTrue(ArraysMDE.any_null(new Object[] { o, null, null }) == true);
      assertTrue(ArraysMDE.any_null(new Object[] { null, o, null }) == true);
      assertTrue(ArraysMDE.any_null(new Object[] { o, null, o }) == true);
      assertTrue(ArraysMDE.any_null(new Object[] { null, o, o }) == true);
      assertTrue(ArraysMDE.any_null(new Object[][] { }) == false);
      assertTrue(ArraysMDE.any_null(new Object[][] { null }) == true);
      assertTrue(ArraysMDE.any_null(new Object[][] { new Object[] { null } }) == false);
      assertTrue(ArraysMDE.any_null(new Object[][] { new Object[] { null }, null }) == true);
      assertTrue(ArraysMDE.any_null(new Object[][] { new Object[] { null }, new Object[] { o } }) == false);
    }

    // public static boolean all_null(Object[] a)
    {
      Object o = new Object();
      assertTrue(ArraysMDE.all_null(new Object[] { }) == true);
      assertTrue(ArraysMDE.all_null(new Object[] { null }) == true);
      assertTrue(ArraysMDE.all_null(new Object[] { null, null }) == true);
      assertTrue(ArraysMDE.all_null(new Object[] { o }) == false);
      assertTrue(ArraysMDE.all_null(new Object[] { o, o }) == false);
      assertTrue(ArraysMDE.all_null(new Object[] { o, null, null }) == false);
      assertTrue(ArraysMDE.all_null(new Object[] { null, o, null }) == false);
      assertTrue(ArraysMDE.all_null(new Object[] { o, null, o }) == false);
      assertTrue(ArraysMDE.all_null(new Object[] { null, o, o }) == false);
      assertTrue(ArraysMDE.all_null(new Object[][] { }) == true);
      assertTrue(ArraysMDE.all_null(new Object[][] { null }) == true);
      assertTrue(ArraysMDE.all_null(new Object[][] { null, null }) == true);
      assertTrue(ArraysMDE.all_null(new Object[][] { new Object[] { null } }) == false);
      assertTrue(ArraysMDE.all_null(new Object[][] { new Object[] { null }, null }) == false);
      assertTrue(ArraysMDE.all_null(new Object[][] { new Object[] { null }, new Object[] { o } }) == false);
    }

  }

  // This cannot be static because it instantiates an inner class.
  public void testHasher() {

    /// To check (maybe some of these are done already).
    /// All of these methods are in Intern; should the tests appear in
    /// testIntern() or here?
    // public static void internStrings(String[] a)
    // public static boolean isInterned(Object value)
    // public static int numIntegers()
    // public static int numIntArrays()
    // public static int numDoubles()
    // public static int numDoubleArrays()
    // public static int numObjectArrays()
    // public static Iterator integers()
    // public static Iterator intArrays()
    // public static Iterator doubles()
    // public static Iterator doubleArrays()
    // public static Iterator objectArrays()
    // public static Integer intern(Integer a)
    // public static Integer internedInteger(int i)
    // public static Integer internedInteger(String s)
    // public static int[] intern(int[] a)
    // public static Double intern(Double a)
    // public static Double internedDouble(int i)
    // public static Double internedDouble(String s)
    // public static double[] intern(double[] a)
    // public static Object[] intern(Object[] a)



    // private static class IntArrayHasher implements Hasher
    // private static class ObjectArrayHasher implements Hasher
    // public static int[] intern(int[] a)
    // public static Object[] intern(Object[] a)

    class InternTest {
      // javadoc won't let this be static.
      void test(boolean random) {
        int size1 = (random ? 100 : 1);
        int size2 = (random ? 10 : 1);

        Random random_gen = new Random();

        int[][] arrays = new int[100][];
        for (int i=0; i<arrays.length; i++) {
          int[] a = new int[10];
          for (int j=0; j<a.length; j++) {
            if (random)
              a[j] = random_gen.nextInt(1000);
            else
              a[j] = j;
          }
          arrays[i] = a;
          // System.out.println(ArraysMDE.toString(a));
          // Sadly, this is required to get the last array to be
          // garbage-collected with Jikes 1.03 and JDK 1.2.2.
          a = null;
        }
        System.gc();
        if (Intern.numIntArrays() != 0)
          throw new Error(" expected 0 int arrays at start, found "
                            + Intern.numIntArrays());
        for (int i=0; i<arrays.length; i++)
          Intern.intern(arrays[i]);
        if (Intern.numIntArrays() != size1)
          throw new Error("Expected " + size1 + ", got " + Intern.numIntArrays() + " int arrays");
        System.gc();
        if (Intern.numIntArrays() != size1)
          throw new Error();
        for (int i=10; i<arrays.length; i++)
          arrays[i] = null;
        System.gc();
        if (Intern.numIntArrays() != size2) {
          if (Intern.numIntArrays() < size2 + 10) {
            System.out.println("Is JIT disabled?  Size should have been " + size2 + ", actually was "
                               + Intern.numIntArrays());
          } else {
            System.out.println("================");
            for (int i=0; i<arrays.length; i++)
              System.out.println(ArraysMDE.toString(arrays[i]));
            System.out.println("================");
            for (Iterator<int[]> itor = Intern.intArrays(); itor.hasNext(); ) {
              System.out.println(ArraysMDE.toString(itor.next()));
            }
            String message = ("Size should have been " + size2 + ", actually was "
                              + Intern.numIntArrays());
            System.out.println(message);
            throw new Error(message);
          }
        }
      }
    }

    InternTest intern = new InternTest();
    intern.test(true);
    intern.test(false);

  }

  public static void testIntern() {
    Integer i = Intern.internedInteger("1234");
    assertTrue(Intern.isInterned(i));
    assertTrue(i.intValue() == 1234);
    i = Intern.internedInteger("0x12ab");
    assertTrue(Intern.isInterned(i));
    assertTrue(i.intValue() == 0x12ab);

    Long l = Intern.internedLong("12345678");
    assertTrue(Intern.isInterned(l));
    assertTrue(l.intValue() == 12345678);
    l = Intern.internedLong("0x1234abcd");
    assertTrue(Intern.isInterned(l));
    assertTrue(l.intValue() == 0x1234abcd);
  }

  // Tests the method "Object intern(Object)" in Intern.java
  public static void testInternObject() {
    Object nIntern = Intern.intern((Object) null);
    assertEquals(null, nIntern);

    String sOrig = new String("foo");
    String sIntern = Intern.intern(sOrig);
    Object sObjIntern = Intern.intern((Object) sOrig);
    assertTrue(sIntern == sObjIntern);
    Object sOtherIntern = Intern.intern(new String("foo"));
    assertTrue(sIntern == sOtherIntern);

    String[] saOrig = new String[] {"foo", "bar"};
    String[] saIntern = Intern.intern(saOrig);
    Object saObjIntern = Intern.intern((Object) saOrig);
    assertTrue(saIntern == saObjIntern);
    Object saOtherIntern = Intern.intern(new String[] {"foo", "bar"});
    assertTrue(saIntern == saOtherIntern);

    Integer iOrig = new Integer(1);
    Integer iIntern = Intern.intern(iOrig);
    Object iObjIntern = Intern.intern((Object) iOrig);
    assertTrue(iIntern == iObjIntern);
    Object iOtherIntern = Intern.intern((Object) new Integer(1));
    assertTrue(iIntern == iOtherIntern);

    Long lOrig = new Long(12345678901234L);
    Long lIntern = Intern.intern(lOrig);
    Object lObjIntern = Intern.intern((Object) lOrig);
    assertTrue(lIntern == lObjIntern);
    Object lOtherIntern = Intern.intern((Object) new Long(12345678901234L));
    assertTrue(lIntern == lOtherIntern);

    int[] iaOrig = new int[] {1, 2, 3};
    int[] iaIntern = Intern.intern(iaOrig);
    Object iaObjIntern = Intern.intern((Object) iaOrig);
    assertTrue(iaIntern == iaObjIntern);
    Object iaOtherIntern = Intern.intern((Object) new int[] {1, 2, 3});
    assertTrue(iaIntern == iaOtherIntern);

    long[] laOrig = new long[] {12345678901234L, 98765432109876L};
    long[] laIntern = Intern.intern(laOrig);
    Object laObjIntern = Intern.intern((Object) laOrig);
    assertTrue(laIntern == laObjIntern);
    Object laOtherIntern = Intern.intern((Object) new long[] {12345678901234L, 98765432109876L});
    assertTrue(laIntern == laOtherIntern);

    // Need to test positive and negative zeros, infinities.

    Double dOrig = new Double(3.14);
    Double dIntern = Intern.intern(dOrig);
    Object dObjIntern = Intern.intern((Object) dOrig);
    assertTrue(dIntern == dObjIntern);
    Object dOtherIntern = Intern.intern((Object) dOrig);
    assertTrue(dIntern == dOtherIntern);

    Double dnOrig = new Double(Double.NaN);
    Double dnIntern = Intern.intern(dnOrig);
    Object dnObjIntern = Intern.intern((Object) dnOrig);
    assertTrue(dnIntern == dnObjIntern);
    Object dnOtherIntern = Intern.intern((Object) new Double(Double.POSITIVE_INFINITY / Double.POSITIVE_INFINITY));
    assertTrue(dnIntern == dnOtherIntern);

    Double diOrig = new Double(Double.POSITIVE_INFINITY);
    Double diIntern = Intern.intern(diOrig);
    Object diObjIntern = Intern.intern((Object) diOrig);
    assertTrue(diIntern == diObjIntern);
    Object diOtherIntern = Intern.intern((Object) new Double(2 * Double.MAX_VALUE));
    assertTrue(diIntern == diOtherIntern);

    double positive_zero = +0.0;
    double negative_zero = -0.0;
    assertTrue(positive_zero == negative_zero);
    assertTrue(1/positive_zero == Double.POSITIVE_INFINITY);
    assertTrue(1/negative_zero == Double.NEGATIVE_INFINITY);

    Double dzOrig = new Double(positive_zero);
    Double dzIntern = Intern.intern(dzOrig);
    Object dzObjIntern = Intern.intern((Object) dzOrig);
    assertTrue(dzIntern == dzObjIntern);
    Object dzOtherIntern = Intern.intern((Object) new Double(negative_zero));
    assertTrue(dzIntern == dzOtherIntern);

    double[] daOrig = new double[] {3.14, 2.71};
    double[] daIntern = Intern.intern(daOrig);
    Object daObjIntern = Intern.intern((Object) daOrig);
    assertTrue(daIntern == daObjIntern);
    Object daOtherIntern = Intern.intern((Object) new double[] {3.14, 2.71});
    assertTrue(daIntern == daOtherIntern);

    double[] da2Orig = new double[] {+0.0, Double.NaN};
    double[] da2Intern = Intern.intern(da2Orig);
    Object da2ObjIntern = Intern.intern((Object) da2Orig);
    assertTrue(da2Intern == da2ObjIntern);
    Object da2OtherIntern = Intern.intern((Object) new double[] {-0.0, Double.POSITIVE_INFINITY / Double.POSITIVE_INFINITY});
    assertTrue(da2Intern == da2OtherIntern);

    Object[] oaOrig = new Object[] {new String("foo"), new Integer(1)};
    Object[] oaIntern = Intern.intern(oaOrig);
    Object oaObjIntern = Intern.intern((Object) oaOrig);
    assertTrue(oaIntern == oaObjIntern);
    Object oaOtherIntern = Intern.intern((Object) new Object[] {new String("foo"), new Integer(1)});
    assertTrue(oaIntern == oaOtherIntern);

    java.awt.Point pOrig = new java.awt.Point(1,2);
    boolean exceptionCaught = false;
    try {
      Intern.intern((Object) pOrig); // performed for side effect
    } catch (IllegalArgumentException e) {
      exceptionCaught = true;
    }
    assertTrue(exceptionCaught);
  }

  // Add elements 0..limit-1 to the set.
  private static void lsis_add_elts(int limit, LimitedSizeIntSet s) {
    Random r = new Random();
    for (int i=0; i<100; i++) {
      s.add(r.nextInt(limit));
    }
  }

  private static void lsis_test(int max_size) {
    LimitedSizeIntSet s = new LimitedSizeIntSet(max_size);
    for (int i=1; i<2*max_size; i++) {
      lsis_add_elts(i, s);
      int size = s.size();
      assertTrue("" + size + " " + i + " " + max_size + " " + s,
                 (i<=max_size) ? (size == i) : (size == max_size+1));
    }
  }

  public static void testLimitedSizeIntSet() {
    for (int i=1; i<10; i++) {
      lsis_test(i);
    }
  }

  // This cannot be static because it instantiates an inner class.
  public void testMathMDE() {

    // int negate(int a)
    assertTrue(MathMDE.negate(3) == -3);
    assertTrue(MathMDE.negate(-22) == 22);
    assertTrue(MathMDE.negate(0) == 0);

    // int bitwiseComplement(int a)
    assertTrue(MathMDE.bitwiseComplement(3) == -4);
    assertTrue(MathMDE.bitwiseComplement(-22) == 21);
    assertTrue(MathMDE.bitwiseComplement(0) == -1);

    // int sign(int a)
    assertTrue(MathMDE.sign(3) == 1);
    assertTrue(MathMDE.sign(-22) == -1);
    assertTrue(MathMDE.sign(0) == 0);

    // int pow(int base, int expt)
    try {
      assertTrue(MathMDE.pow(3, 3) == 27);
      assertTrue(MathMDE.pow(-5, 5) == -3125);
      assertTrue(MathMDE.pow(22, 0) == 1);
      assertTrue(MathMDE.pow(4, 6) == 4096);
      assertTrue(MathMDE.pow(1, 222222) == 1);
      assertTrue(MathMDE.pow(-2, 25) == -33554432);
      // This is beyond the precision.  Maybe return a long instead of an int?
      // assertTrue(MathMDE.pow(-3, 25) == ...);
    } catch (Exception e) {
      e.printStackTrace();
      throw new Error(e);
    }
    {
      boolean exception = false;
      try {
        MathMDE.pow(3, -3);
      } catch (Exception e) {
        exception = true;
      }
      assertTrue(exception);
    }


    // int gcd(int a, int b)
    assertTrue(MathMDE.gcd(2, 50) == 2);
    assertTrue(MathMDE.gcd(50, 2) == 2);
    assertTrue(MathMDE.gcd(12, 144) == 12);
    assertTrue(MathMDE.gcd(144, 12) == 12);
    assertTrue(MathMDE.gcd(96, 144) == 48);
    assertTrue(MathMDE.gcd(144, 96) == 48);
    assertTrue(MathMDE.gcd(10, 25) == 5);
    assertTrue(MathMDE.gcd(25, 10) == 5);
    assertTrue(MathMDE.gcd(17, 25) == 1);
    assertTrue(MathMDE.gcd(25, 17) == 1);
    assertTrue(MathMDE.gcd(0,10) == 10);
    assertTrue(MathMDE.gcd(10,0) == 10);
    assertTrue(MathMDE.gcd(25, -10) == 5);
    assertTrue(MathMDE.gcd(-25, -10) == 5);
    assertTrue(MathMDE.gcd(-25, 10) == 5);

    // int gcd(int[] a)
    assertTrue(MathMDE.gcd(new int[] {2, 50}) == 2);
    assertTrue(MathMDE.gcd(new int[] {12, 144}) == 12);
    assertTrue(MathMDE.gcd(new int[] {96, 144}) == 48);
    assertTrue(MathMDE.gcd(new int[] {10, 25}) == 5);
    assertTrue(MathMDE.gcd(new int[] {100, 10, 25}) == 5);
    assertTrue(MathMDE.gcd(new int[] {768, 324}) == 12);
    assertTrue(MathMDE.gcd(new int[] {2400, 48, 36}) == 12);
    assertTrue(MathMDE.gcd(new int[] {2400, 72, 36}) == 12);

    // int gcd_differences(int[] a)
    // Weak set of tests, derived directly from those of "int gcd(int[] a)".
    assertTrue(MathMDE.gcd_differences(new int[] {0, 2, 52}) == 2);
    assertTrue(MathMDE.gcd_differences(new int[] {0, 12, 156}) == 12);
    assertTrue(MathMDE.gcd_differences(new int[] {0, 96, 240}) == 48);
    assertTrue(MathMDE.gcd_differences(new int[] {0, 10, 35}) == 5);
    assertTrue(MathMDE.gcd_differences(new int[] {0, 100, 110, 135}) == 5);
    assertTrue(MathMDE.gcd_differences(new int[] {0, 768, 1092}) == 12);
    assertTrue(MathMDE.gcd_differences(new int[] {0, 2400, 2448, 2484}) == 12);
    assertTrue(MathMDE.gcd_differences(new int[] {0, 2400, 2472, 2508}) == 12);

    // int mod_positive(int x, int y)
    assertTrue(MathMDE.mod_positive(33, 5) == 3);
    assertTrue(MathMDE.mod_positive(-33, 5) == 2);
    assertTrue(MathMDE.mod_positive(33, -5) == 3);
    assertTrue(MathMDE.mod_positive(-33, -5) == 2);

    // int[] missing_numbers(int[] nums)
    assert_arrays_equals(MathMDE.missing_numbers(new int[] { 3,4,5,6,7,8 }),
                         new int[] {});
    assert_arrays_equals(MathMDE.missing_numbers(new int[] { 3,4,6,7,8 }),
                         new int[] { 5 });
    assert_arrays_equals(MathMDE.missing_numbers(new int[] { 3,4,8 }),
                         new int[] { 5,6,7 });
    assert_arrays_equals(MathMDE.missing_numbers(new int[] { 3,5,6,8 }),
                         new int[] { 4,7 });
    assert_arrays_equals(MathMDE.missing_numbers(new int[] { 3,6,8 }),
                         new int[] { 4,5,7 });


    // class MissingNumbersIteratorInt
    class TestMissingNumbersIteratorInt {
      // javadoc won't let this be static
      void test(int[] orig, boolean add_ends, int[] goal_missing) {
        Iterator<Integer> orig_iterator = int_array_iterator(orig);
        Iterator<Integer> missing_iterator = new MathMDE.MissingNumbersIteratorInt(orig_iterator, add_ends);
        int[] missing = TestUtilMDE.int_iterator_array(missing_iterator);
        assert_arrays_equals(missing, goal_missing);
      }
    }

    TestMissingNumbersIteratorInt tmni = new TestMissingNumbersIteratorInt();
    tmni.test(new int[] { 3,4,5,6,7,8 }, false, new int[] {});
    tmni.test(new int[] { 3,4,6,7,8 }, false, new int[] { 5 });
    tmni.test(new int[] { 3,4,8 }, false, new int[] { 5,6,7 });
    tmni.test(new int[] { 3,5,6,8 }, false, new int[] { 4,7 });
    tmni.test(new int[] { 3,6,8 }, false, new int[] { 4,5,7 });
    tmni.test(new int[] { 3 }, false, new int[] { });
    tmni.test(new int[] { 3,4,5 }, false, new int[] { });
    tmni.test(new int[] { 3,4,5,6,7,8 }, true, new int[] { 2,9 });
    tmni.test(new int[] { 3,4,6,7,8 }, true, new int[] { 2,5,9 });
    tmni.test(new int[] { 3,4,8 }, true, new int[] { 2,5,6,7,9 });
    tmni.test(new int[] { 3,5,6,8 }, true, new int[] { 2,4,7,9 });
    tmni.test(new int[] { 3,6,8 }, true, new int[] { 2,4,5,7,9 });
    tmni.test(new int[] { 3,4,5 }, true, new int[] { 2,6 });

    tmni.test(new int[] { -1,1,2,3,5,6,7,9 }, true, new int[] { -2,0,4,8,10 });


    // int[] modulus(int[] nums)
    // int[] modulus(Iterator itor)

    class TestModulus {
      // javadoc won't let this be static
      void check(int[] nums, int[] goal_rm) {
        int[] rm = MathMDE.modulus(nums);
        if (!Arrays.equals(rm, goal_rm))
          throw new Error("Expected (r,m)=" + ArraysMDE.toString(goal_rm)
                          + ", saw (r,m)=" + ArraysMDE.toString(rm));
        if (rm == null)
          return;
        int goal_r = rm[0];
        int m = rm[1];
        for (int i=0; i<nums.length; i++) {
          int r = nums[i] % m;
          if (r < 0) r += m;
          if (r != goal_r)
            throw new Error("Expected " + nums[i] + " % " + m + " = " + goal_r
                            + ", got " + r);
        }
      }

      // javadoc won't let this be static
      void check(Iterator<Integer> itor, int[] goal_rm) {
        // There would be no point to this:  it's testing
        // int_iterator_array, not the iterator version!
        // return check(int_iterator_array(itor), goal_rm);
        assert_arrays_equals(MathMDE.modulus_int(itor), goal_rm);
      }

      // javadoc won't let this be static
      void check_iterator(int[] nums, int[] goal_rm) {
        check(int_array_iterator(nums), goal_rm);
      }
    }

    TestModulus testModulus = new TestModulus();

    testModulus.check(new int[] {3,7,47,51}, new int[] { 3,4 });
    testModulus.check(new int[] {3,11,43,51}, new int[] { 3,8 });
    testModulus.check(new int[] {3,11,47,55}, new int[] { 3,4 });
    testModulus.check(new int[] {2383,4015,-81,463,-689}, new int[] { 15,32 });

    testModulus.check_iterator(new int[] {3,7,47,51}, new int[] { 3,4 });
    testModulus.check_iterator(new int[] {3,11,43,51}, new int[] { 3,8 });
    testModulus.check_iterator(new int[] {3,11,47,55}, new int[] { 3,4 });
    testModulus.check_iterator(new int[] {2383,4015,-81,463,-689}, new int[] { 15,32 });


    // int[] nonmodulus_strict(int[] nums)
    // int[] nonmodulus_nonstrict(int[] nums)
    // int[] nonmodulus_strict(Iterator nums)

    class TestNonModulus {
      // javadoc won't let this be static
      void check_strict(int[] nums, int[] goal_rm) {
        check(nums, goal_rm, true);
        Iterator<Integer> itor = int_array_iterator(nums);
        assert_arrays_equals(MathMDE.nonmodulus_strict_int(itor), goal_rm);
      }

      // javadoc won't let this be static
      void check_nonstrict(int[] nums, int[] goal_rm) {
        check(nums, goal_rm, false);
      }

      // javadoc won't let this be static
      void check(int[] nums, int[] goal_rm, boolean strict) {
        int[] rm;
        if (strict)
          rm = MathMDE.nonmodulus_strict(nums);
        else
          rm = MathMDE.nonmodulus_nonstrict(nums);
        if (!Arrays.equals(rm, goal_rm))
          throw new Error("Expected (r,m)=" + ArraysMDE.toString(goal_rm)
                          + ", saw (r,m)=" + ArraysMDE.toString(rm));
        if (rm == null)
          return;
        int goal_r = rm[0];
        int m = rm[1];
        for (int i=0; i<nums.length; i++) {
          int r = nums[i] % m;
          if (r < 0) r += m;
          if (r == goal_r)
            throw new Error("Expected inequality, saw " + nums[i] + " % " + m + " = " + r);
        }
      }
    }

    TestNonModulus testNonModulus = new TestNonModulus();

    testNonModulus.check_strict(new int[] {1,2,3,5,6,7,9}, null);
    testNonModulus.check_strict(new int[] {-1,1,2,3,5,6,7,9}, new int[] {0,4});
    testNonModulus.check_strict(new int[] {1,2,3,5,6,7,9,11}, null);
    testNonModulus.check_strict(new int[] {1,2,3,5,6,7,11}, null);
    testNonModulus.check_strict(new int[] {1,2,4,6,8,10}, null);

    // null because only 7 elements, so don't try modulus = 4
    testNonModulus.check_nonstrict(new int[] {1,2,3,5,6,7,9}, null);
    testNonModulus.check_nonstrict(new int[] {1,2,3,5,6,7,9,10}, new int[] {0,4});
    testNonModulus.check_nonstrict(new int[] {1,2,3,5,6,7,9,11}, new int[] {0,4});
    testNonModulus.check_nonstrict(new int[] {1,2,3,5,6,7,9,11,12,13}, null);
    testNonModulus.check_nonstrict(new int[] {1,2,3,5,6,7,9,11,12,13,14,15}, new int[] {4,6});
    testNonModulus.check_nonstrict(new int[] {1,2,3,5,6,7,9,11,12,13,14,15,22}, null);

  }

  public static void testOrderedPairIterator() {
    final int NULL = -2222;

    Vector<Integer> ones = new Vector<Integer>();
    for (int i=1; i<=30; i++)
      ones.add(new Integer(i));
    Vector<Integer> twos = new Vector<Integer>();
    for (int i=2; i<=30; i+=2)
      twos.add(new Integer(i));
    Vector<Integer> threes = new Vector<Integer>();
    for (int i=3; i<=30; i+=3)
      threes.add(new Integer(i));

    // I've replaced the nulls by 0 in order to permit the array elements
    // to be ints instead of Integers.

    compareOrderedPairIterator(new OrderedPairIterator<Integer>(ones.iterator(), ones.iterator()),
                               new int[][] { {1, 1}, {2, 2}, {3, 3}, {4, 4}, {5, 5}, {6, 6}, {7, 7}, {8, 8}, {9, 9}, {10, 10}, {11, 11}, {12, 12}, {13, 13}, {14, 14}, {15, 15}, {16, 16}, {17, 17}, {18, 18}, {19, 19}, {20, 20}, {21, 21}, {22, 22}, {23, 23}, {24, 24}, {25, 25}, {26, 26}, {27, 27}, {28, 28}, {29, 29}, {30, 30}, });

    compareOrderedPairIterator(new OrderedPairIterator<Integer>(ones.iterator(), twos.iterator()),
                               new int[][] { {1, NULL}, {2, 2}, {3, NULL}, {4, 4}, {5, NULL}, {6, 6}, {7, NULL}, {8, 8}, {9, NULL}, {10, 10}, {11, NULL}, {12, 12}, {13, NULL}, {14, 14}, {15, NULL}, {16, 16}, {17, NULL}, {18, 18}, {19, NULL}, {20, 20}, {21, NULL}, {22, 22}, {23, NULL}, {24, 24}, {25, NULL}, {26, 26}, {27, NULL}, {28, 28}, {29, NULL}, {30, 30}, });

    compareOrderedPairIterator(new OrderedPairIterator<Integer>(twos.iterator(), ones.iterator()),
                               new int[][] { {NULL, 1}, {2, 2}, {NULL, 3}, {4, 4}, {NULL, 5}, {6, 6}, {NULL, 7}, {8, 8}, {NULL, 9}, {10, 10}, {NULL, 11}, {12, 12}, {NULL, 13}, {14, 14}, {NULL, 15}, {16, 16}, {NULL, 17}, {18, 18}, {NULL, 19}, {20, 20}, {NULL, 21}, {22, 22}, {NULL, 23}, {24, 24}, {NULL, 25}, {26, 26}, {NULL, 27}, {28, 28}, {NULL, 29}, {30, 30}, });

    compareOrderedPairIterator(new OrderedPairIterator<Integer>(ones.iterator(), threes.iterator()),
                               new int[][] { {1, NULL}, {2, NULL}, {3, 3}, {4, NULL}, {5, NULL}, {6, 6}, {7, NULL}, {8, NULL}, {9, 9}, {10, NULL}, {11, NULL}, {12, 12}, {13, NULL}, {14, NULL}, {15, 15}, {16, NULL}, {17, NULL}, {18, 18}, {19, NULL}, {20, NULL}, {21, 21}, {22, NULL}, {23, NULL}, {24, 24}, {25, NULL}, {26, NULL}, {27, 27}, {28, NULL}, {29, NULL}, {30, 30}, });

    compareOrderedPairIterator(new OrderedPairIterator<Integer>(twos.iterator(), threes.iterator()),
                               new int[][] { {2, NULL}, {NULL, 3}, {4, NULL}, {6, 6}, {8, NULL}, {NULL, 9}, {10, NULL}, {12, 12}, {14, NULL}, {NULL, 15}, {16, NULL}, {18, 18}, {20, NULL}, {NULL, 21}, {22, NULL}, {24, 24}, {26, NULL}, {NULL, 27}, {28, NULL}, {30, 30}, });

  }

  public static void compareOrderedPairIterator(OrderedPairIterator<Integer> opi, int[][] ints) {
    int pairno = 0;
    while (opi.hasNext()) {
      Pair<Integer,Integer> pair = opi.next();
      // System.out.println("Iterator: <" + pair.a + "," + pair.b + ">, array: <" + ints[pairno][0] + "," + ints[pairno][1] + ">");
      assertTrue((pair.a == null) || (pair.a.intValue() == ints[pairno][0]));
      assertTrue((pair.b == null) || (pair.b.intValue() == ints[pairno][1]));
      pairno++;
    }
    assertTrue(pairno == ints.length);
  }

  private static BitSet randomBitSet(int length, Random r) {
    BitSet result = new BitSet(length);
    for (int i=0; i<length; i++) {
      result.set(i, r.nextBoolean());
    }
    return result;
  }

  public void testStringBuilderDelimited() {
    compareJoinAndSBD(new String[] { "foo", "bar", "baz" });
    compareJoinAndSBD(new String[] { "foo" });
    compareJoinAndSBD(new String[] { });
  }

  public void compareJoinAndSBD(String[] strings) {
    StringBuilderDelimited sbd = new StringBuilderDelimited(",");
    for (String str: strings) {
      sbd.append(str);
    }
    assertTrue(sbd.toString().equals(UtilMDE.join(strings, ",")));
  }


  // This cannot be static because it instantiates an inner class.
  public void testUtilMDE() {

    // public static intersectionCardinalityAtLeast(BitSet a, BitSet b, int i)
    {
      Random r = new Random(20031008);
      for (int i=0; i<100; i++) {
        BitSet b1 = randomBitSet(r.nextInt(100), r);
        BitSet b2 = randomBitSet(r.nextInt(100), r);
        BitSet b3 = randomBitSet(r.nextInt(100), r);
        BitSet intersection = (BitSet) b1.clone();
        intersection.and(b2);
        int card = intersection.cardinality();
        for (int j=0; j<100; j++) {
          Assert.assertTrue(UtilMDE.intersectionCardinalityAtLeast(b1, b2, j) == (card >= j));
        }
        intersection.and(b3);
        card = intersection.cardinality();
        for (int j=0; j<100; j++) {
          Assert.assertTrue(UtilMDE.intersectionCardinalityAtLeast(b1, b2, b3, j) == (card >= j));
        }
      }
    }

    // public static BufferedReader bufferedFileReader(String filename)
    // public static LineNumberReader lineNumberFileReader(String filename)
    // public static BufferedWriter bufferedFileWriter(String filename) throws IOException
    // public static Class classForName(String className)

    // public static String classnameToJvm(String classname)
    Assert.assertTrue(UtilMDE.classnameToJvm("boolean").equals("Z"));
    Assert.assertTrue(UtilMDE.classnameToJvm("byte").equals("B"));
    Assert.assertTrue(UtilMDE.classnameToJvm("char").equals("C"));
    Assert.assertTrue(UtilMDE.classnameToJvm("double").equals("D"));
    Assert.assertTrue(UtilMDE.classnameToJvm("float").equals("F"));
    Assert.assertTrue(UtilMDE.classnameToJvm("int").equals("I"));
    Assert.assertTrue(UtilMDE.classnameToJvm("long").equals("J"));
    Assert.assertTrue(UtilMDE.classnameToJvm("short").equals("S"));
    Assert.assertTrue(UtilMDE.classnameToJvm("Integer").equals("LInteger;"));
    Assert.assertTrue(UtilMDE.classnameToJvm("Java.lang.Integer").equals("LJava/lang/Integer;"));
    Assert.assertTrue(UtilMDE.classnameToJvm("Java.lang.Integer[][][]").equals("[[[LJava/lang/Integer;"));

    // public static String arglistToJvm(String arglist)
    Assert.assertTrue(UtilMDE.arglistToJvm("()").equals("()"));
    Assert.assertTrue(UtilMDE.arglistToJvm("(int)").equals("(I)"));
    Assert.assertTrue(UtilMDE.arglistToJvm("(int, int)").equals("(II)"));
    Assert.assertTrue(UtilMDE.arglistToJvm("(int, long, short)").equals("(IJS)"));
    Assert.assertTrue(UtilMDE.arglistToJvm("(java.lang.Integer, int, java.lang.Integer)").equals("(Ljava/lang/Integer;ILjava/lang/Integer;)"));
    Assert.assertTrue(UtilMDE.arglistToJvm("(int[])").equals("([I)"));
    Assert.assertTrue(UtilMDE.arglistToJvm("(int[], int, int)").equals("([III)"));
    Assert.assertTrue(UtilMDE.arglistToJvm("(int, int[][], int)").equals("(I[[II)"));
    Assert.assertTrue(UtilMDE.arglistToJvm("(java.lang.Integer[], int, java.lang.Integer[][])").equals("([Ljava/lang/Integer;I[[Ljava/lang/Integer;)"));

    // public static String classnameFromJvm(String classname)
    Assert.assertTrue(UtilMDE.classnameFromJvm("Z").equals("boolean"));
    Assert.assertTrue(UtilMDE.classnameFromJvm("B").equals("byte"));
    Assert.assertTrue(UtilMDE.classnameFromJvm("C").equals("char"));
    Assert.assertTrue(UtilMDE.classnameFromJvm("D").equals("double"));
    Assert.assertTrue(UtilMDE.classnameFromJvm("F").equals("float"));
    Assert.assertTrue(UtilMDE.classnameFromJvm("I").equals("int"));
    Assert.assertTrue(UtilMDE.classnameFromJvm("J").equals("long"));
    Assert.assertTrue(UtilMDE.classnameFromJvm("S").equals("short"));
    Assert.assertTrue(UtilMDE.classnameFromJvm("LInteger;").equals("Integer"));
    Assert.assertTrue(UtilMDE.classnameFromJvm("LJava/lang/Integer;").equals("Java.lang.Integer"));
    Assert.assertTrue(UtilMDE.classnameFromJvm("[[LJava/lang/Integer;").equals("Java.lang.Integer[][]"));

    // public static String arglistFromJvm(String arglist)
    Assert.assertTrue(UtilMDE.arglistFromJvm("()").equals("()"));
    Assert.assertTrue(UtilMDE.arglistFromJvm("(I)").equals("(int)"));
    Assert.assertTrue(UtilMDE.arglistFromJvm("(II)").equals("(int, int)"));
    Assert.assertTrue(UtilMDE.arglistFromJvm("(IJS)").equals("(int, long, short)"));
    Assert.assertTrue(UtilMDE.arglistFromJvm("(Ljava/lang/Integer;ILjava/lang/Integer;)").equals("(java.lang.Integer, int, java.lang.Integer)"));
    Assert.assertTrue(UtilMDE.arglistFromJvm("([I)").equals("(int[])"));
    Assert.assertTrue(UtilMDE.arglistFromJvm("([III)").equals("(int[], int, int)"));
    Assert.assertTrue(UtilMDE.arglistFromJvm("(I[[II)").equals("(int, int[][], int)"));
    Assert.assertTrue(UtilMDE.arglistFromJvm("([Ljava/lang/Integer;I[[Ljava/lang/Integer;)").equals("(java.lang.Integer[], int, java.lang.Integer[][])"));


    // public static void addToClasspath(String dir)
    // public static final class WildcardFilter implements FilenameFilter
    //   public WildcardFilter(String filename)
    //   public boolean accept(File dir, String name)
    // public static boolean canCreateAndWrite(File file)
    // public static void writeObject(Object o, File file) throws IOException
    // public static Object readObject(File file)
    // public static File createTempDir(String prefix, String suffix)

    // public Object incrementHashMap(HashMap hm, Object key, int count)

    try {
      junit.framework.Assert.assertEquals(true,
        UtilMDE.canCreateAndWrite(new File("TestUtilMDE.java")));

      // This test fails if run by the superuser (who can overwrite
      // any file).
      if (! System.getProperty("user.name").equals("root")) {
        File readOnly = new File("temp");
        readOnly.createNewFile();
        readOnly.setReadOnly();
        junit.framework.Assert.assertEquals(false,
          UtilMDE.canCreateAndWrite(readOnly));
        readOnly.delete();
      }

      junit.framework.Assert.assertEquals(true,
        UtilMDE.canCreateAndWrite(new File("temp")));
      junit.framework.Assert.assertEquals(false,
        UtilMDE.canCreateAndWrite(new File("temp/temp")));
    } catch (IOException e) {
      e.printStackTrace();
      junit.framework.Assert.fail("failure while testing UtilMDE.canCreateAndWrite(): " + e.toString());
    }

    {
      // These names are taken from APL notation, where iota creates an
      // array of all the numbers up to its argument.
      Vector<Integer> iota0 = new Vector<Integer>();
      Vector<Integer> iota10 = new Vector<Integer>();
      for (int i=0; i<10; i++)
        iota10.add(new Integer(i));
      Vector<Integer> iota10_twice = new Vector<Integer>();
      iota10_twice.addAll(iota10);
      iota10_twice.addAll(iota10);
      Vector<Integer> iota10_thrice = new Vector<Integer>();
      iota10_thrice.addAll(iota10);
      iota10_thrice.addAll(iota10);
      iota10_thrice.addAll(iota10);

      // public static class EnumerationIterator implements Iterator
      // public static class IteratorEnumeration implements Enumeration

      Assert.assertTrue(iota0.equals(toVector(iota0.iterator())));
      Assert.assertTrue(iota0.equals(toVector(new UtilMDE.IteratorEnumeration<Integer>(iota0.iterator()))));
      Assert.assertTrue(iota0.equals(toVector(iota0.elements())));
      Assert.assertTrue(iota0.equals(toVector(new UtilMDE.EnumerationIterator<Integer>(iota0.elements()))));
      Assert.assertTrue(iota10.equals(toVector(iota10.iterator())));
      Assert.assertTrue(iota10.equals(toVector(new UtilMDE.IteratorEnumeration<Integer>(iota10.iterator()))));
      Assert.assertTrue(iota10.equals(toVector(iota10.elements())));
      Assert.assertTrue(iota10.equals(toVector(new UtilMDE.EnumerationIterator<Integer>(iota10.elements()))));

      // public static class MergedIterator2 implements Iterator {
      Assert.assertTrue(iota10_twice.equals(toVector(new UtilMDE.MergedIterator2<Integer>(iota10.iterator(), iota10.iterator()))));
      Assert.assertTrue(iota10.equals(toVector(new UtilMDE.MergedIterator2<Integer>(iota0.iterator(), iota10.iterator()))));
      Assert.assertTrue(iota10.equals(toVector(new UtilMDE.MergedIterator2<Integer>(iota10.iterator(), iota0.iterator()))));

      // public static class MergedIterator implements Iterator {
      Vector<Iterator<Integer>> iota10_iterator_thrice = new Vector<Iterator<Integer>>();
      iota10_iterator_thrice.add(iota10.iterator());
      iota10_iterator_thrice.add(iota10.iterator());
      iota10_iterator_thrice.add(iota10.iterator());
      Assert.assertTrue(iota10_thrice.equals(toVector(new UtilMDE.MergedIterator<Integer>(iota10_iterator_thrice.iterator()))));
      Vector<Iterator<Integer>> iota10_iterator_twice_1 = new Vector<Iterator<Integer>>();
      iota10_iterator_twice_1.add(iota0.iterator());
      iota10_iterator_twice_1.add(iota10.iterator());
      iota10_iterator_twice_1.add(iota10.iterator());
      Vector<Iterator<Integer>> iota10_iterator_twice_2 = new Vector<Iterator<Integer>>();
      iota10_iterator_twice_2.add(iota10.iterator());
      iota10_iterator_twice_2.add(iota0.iterator());
      iota10_iterator_twice_2.add(iota10.iterator());
      Vector<Iterator<Integer>> iota10_iterator_twice_3 = new Vector<Iterator<Integer>>();
      iota10_iterator_twice_3.add(iota10.iterator());
      iota10_iterator_twice_3.add(iota10.iterator());
      iota10_iterator_twice_3.add(iota0.iterator());
      Assert.assertTrue(iota10_twice.equals(toVector(new UtilMDE.MergedIterator<Integer>(iota10_iterator_twice_1.iterator()))));
      Assert.assertTrue(iota10_twice.equals(toVector(new UtilMDE.MergedIterator<Integer>(iota10_iterator_twice_2.iterator()))));
      Assert.assertTrue(iota10_twice.equals(toVector(new UtilMDE.MergedIterator<Integer>(iota10_iterator_twice_3.iterator()))));


      class OddFilter implements Filter<Integer> {
        public OddFilter() { }
        public boolean accept(Integer i) {
          return i.intValue() % 2 == 1;
        }
      }

      // public static final class FilteredIterator implements Iterator

      Vector<Integer> iota10_odd = new Vector<Integer>();
      for (int i=0; i<iota10.size(); i++)
        if (i%2 == 1)
          iota10_odd.add(new Integer(i));
      Assert.assertTrue(iota10_odd.equals(toVector(new UtilMDE.FilteredIterator<Integer>(iota10.iterator(), new OddFilter()))));

    }

    // public static final class RemoveFirstAndLastIterator implements Iterator
    {
      Vector<Integer> iota5 = new Vector<Integer>();
      for (int i=0; i<5; i++)
        iota5.add(new Integer(i));
      Vector<Integer> iota5middle = new Vector<Integer>();
      for (int i=1; i<4; i++)
        iota5middle.add(new Integer(i));
      UtilMDE.RemoveFirstAndLastIterator<Integer> rfali = new UtilMDE.RemoveFirstAndLastIterator<Integer>(iota5.iterator());
      Vector<Integer> rfali_vector = toVector(rfali);
      Assert.assertTrue(iota5middle.equals(rfali_vector));
      Assert.assertTrue(rfali.getFirst().equals(new Integer(0)));
      Assert.assertTrue(rfali.getLast().equals(new Integer(4)));
    }

    // public static ArrayList randomElements(Iterator itor, int num_elts)
    // public static ArrayList randomElements(Iterator itor, int num_elts, Random random)

    // Iterate through numbers from zero up to the argument (non-inclusive)
    class IotaIterator implements Iterator<Integer> {
      int i = 0;
      int limit;
      public IotaIterator(int limit) { this.limit = limit; }
      public boolean hasNext() { return i<limit; }
      public Integer next() {
        if (! hasNext()) throw new NoSuchElementException();
        return new Integer(i++);
      }
      public void remove() { throw new UnsupportedOperationException(); }
    }
    {
      int itor_size = 10;
      int num_elts_limit = 12;
      int tries = 100000;
      Random r = new Random(20020311);
      // System.out.println();
      // "i++" here works, but is slow
      for (int i=1; i<num_elts_limit; i+=3) {
        int[] totals = new int[num_elts_limit];
        for (int j=0; j<tries; j++) {
          List<Integer> chosen = UtilMDE.randomElements(new IotaIterator(itor_size), i, r);
          for (int m=0; m<chosen.size(); m++) {
            for (int n=m+1; n<chosen.size(); n++) {
              if ( chosen.get(m).intValue() == chosen.get(n).intValue() ) {
                throw new Error("Duplicate at " + m + "," + n);
              }
            }
          }
          for (int k=0; k<chosen.size(); k++) {
            totals[chosen.get(k).intValue()]++;
          }
        }
        int i_truncated = Math.min(itor_size, i);
        int grand_total = tries * i_truncated;
        Assert.assertTrue(ArraysMDE.sum(totals) == grand_total, "Totals = " + ArraysMDE.sum(totals));
        // System.out.print("chosen:\t");
        for (int k=0; k<num_elts_limit; k++) {
          int this_total = totals[k];
          int expected = tries * i_truncated / itor_size;
          double ratio = (double)this_total / (double)expected;
          // System.out.print(((k<10) ? " " : "") + k + " " + this_total + "\t");
          // System.out.print("exp=" + expected + "\t" + "ratio=" + ratio + "\t");
          Assert.assertTrue(k >= itor_size || (ratio > .98 && ratio < 1.02));
        }
        // System.out.println();
      }
    }


    // public static Method methodForName(String methodname) throws ClassNotFoundException
//
    // essentially I am just testing whether the return is erroneous
    try {
      assertTrue(null != UtilMDE.methodForName("utilMDE.UtilMDE.methodForName(java.lang.String, java.lang.String, java.lang.Class[])"));
      assertTrue(null != UtilMDE.methodForName("utilMDE.UtilMDE.methodForName(java.lang.String,java.lang.String,java.lang.Class[])"));
      assertTrue(null != UtilMDE.methodForName("java.lang.Math.min(int,int)"));
    } catch (Exception e) {
      e.printStackTrace();
      throw new Error(e);
    }
    try {
      java.lang.reflect.Method m = UtilMDE.methodForName("utilMDE.UtilMDE.methodForName()");
    } catch (NoSuchMethodException e) {
      // nothing to do; this is the expected case
    } catch (Exception e) {
      e.printStackTrace();
      throw new Error(e);
    }


    // public static boolean propertyIsTrue(Properties p, String key)
    // public static String appendProperty(Properties p, String key, String value)
    // public static String setDefault(Properties p, String key, String value)
    // public static void streamCopy(java.io.InputStream from, java.io.OutputStream to)

    // public static String replaceString(String target, String oldStr, String newStr)

    assertTrue(UtilMDE.replaceString("hello dolly well hello dolly", " ", "  ").equals("hello  dolly  well  hello  dolly"));
    assertTrue(UtilMDE.replaceString("  hello  dolly well hello dolly  ", " ", "  ").equals("    hello    dolly  well  hello  dolly    "));
    assertTrue(UtilMDE.replaceString("hello dolly well hello dolly", "ll", "y").equals("heyo doyy wey heyo doyy"));
    assertTrue(UtilMDE.replaceString("hello dolly well hello dolly", "q", "xxx").equals("hello dolly well hello dolly"));

    // public static String[] split(String s, char delim)
    // public static String[] split(String s, String delim)

    assertTrue(Arrays.equals(UtilMDE.split("foo,bar,baz", ','), new String[] { "foo", "bar", "baz" }));
    assertTrue(Arrays.equals(UtilMDE.split("foo", ','), new String[] { "foo" }));
    assertTrue(Arrays.equals(UtilMDE.split("", ','), new String[] { "" }));
    assertTrue(Arrays.equals(UtilMDE.split(",foo,", ','), new String[] { "", "foo", "" }));
    assertTrue(Arrays.equals(UtilMDE.split("foo,bar,baz", ","), new String[] { "foo", "bar", "baz" }));
    assertTrue(Arrays.equals(UtilMDE.split("foo", ","), new String[] { "foo" }));
    assertTrue(Arrays.equals(UtilMDE.split("", ","), new String[] { "" }));
    assertTrue(Arrays.equals(UtilMDE.split(",foo,", ","), new String[] { "", "foo", "" }));
    assertTrue(Arrays.equals(UtilMDE.split("foo, bar, baz", ", "), new String[] { "foo", "bar", "baz" }));
    assertTrue(Arrays.equals(UtilMDE.split("foo", ", "), new String[] { "foo" }));
    assertTrue(Arrays.equals(UtilMDE.split("", ", "), new String[] { "" }));
    assertTrue(Arrays.equals(UtilMDE.split(", foo, ", ", "), new String[] { "", "foo", "" }));

    // public static String join(Object[] a, String delim)
    // public static String join(Vector v, String delim)

    assertTrue(UtilMDE.join(new String[] { "foo", "bar", "baz" }, ", ").equals("foo, bar, baz"));
    assertTrue(UtilMDE.join(new String[] { "foo" }, ", ").equals("foo"));
    assertTrue(UtilMDE.join(new String[] { }, ", ").equals(""));
    assertTrue(UtilMDE.join(new Integer[] { new Integer(0), new Integer(1), new Integer(2), new Integer(3), new Integer(4) }, "").equals("01234"));
    Vector<Object> potpourri = new Vector<Object>();
    potpourri.add("day"); potpourri.add(new Integer(2)); potpourri.add("day");
    assertTrue(UtilMDE.join(potpourri, " ").equals("day 2 day"));

    // public static String escapeNonJava(String orig)
    // public static String escapeNonJava(Character ch)

    assertTrue(UtilMDE.escapeNonJava("foobar").equals("foobar"));
    assertTrue(UtilMDE.escapeNonJava("").equals(""));
    assertTrue(UtilMDE.escapeNonJava("\\").equals("\\\\"));
    assertTrue(UtilMDE.escapeNonJava("\\\n\r\"").equals("\\\\\\n\\r\\\""));
    assertTrue(UtilMDE.escapeNonJava("split\nlines").equals("split\\nlines"));
    assertTrue(UtilMDE.escapeNonJava("\\relax").equals("\\\\relax"));
    assertTrue(UtilMDE.escapeNonJava("\"hello\"").equals("\\\"hello\\\""));
    assertTrue(UtilMDE.escapeNonJava("\"hello\" \"world\"")
               .equals("\\\"hello\\\" \\\"world\\\""));

    // public static String escapeNonASCII(String orig)

    assertTrue(UtilMDE.escapeNonASCII("foobar").equals("foobar"));
    assertTrue(UtilMDE.escapeNonASCII("").equals(""));
    assertTrue(UtilMDE.escapeNonASCII("\\").equals("\\\\"));
    assertTrue(UtilMDE.escapeNonASCII("\\\n\r\"").equals("\\\\\\n\\r\\\""));
    assertTrue(UtilMDE.escapeNonASCII("split\nlines").equals("split\\nlines"));
    assertTrue(UtilMDE.escapeNonASCII("\\relax").equals("\\\\relax"));
    assertTrue(UtilMDE.escapeNonASCII("\"hello\"").equals("\\\"hello\\\""));
    assertTrue(UtilMDE.escapeNonASCII("\"hello\" \"world\"")
               .equals("\\\"hello\\\" \\\"world\\\""));
    assertTrue(UtilMDE.escapeNonASCII("\0\1\2\7\12\70\100\111\222")
               .equals("\\000\\001\\002\\007\\n8@I\\222"));
    assertTrue(UtilMDE.escapeNonASCII("\u0100\u1000\ucafe\uffff")
               .equals("\\u0100\\u1000\\ucafe\\uffff"));

    // private static String escapeNonASCII(char c)


    // public static String unescapeNonJava(String orig)

    assertTrue(UtilMDE.unescapeNonJava("foobar").equals("foobar"));
    assertTrue(UtilMDE.unescapeNonJava("").equals(""));
    assertTrue(UtilMDE.unescapeNonJava("\\\\").equals("\\"));
    assertTrue(UtilMDE.unescapeNonJava("\\\"").equals("\""));
    assertTrue(UtilMDE.unescapeNonJava("\\n").equals("\n")); // not lineSep
    assertTrue(UtilMDE.unescapeNonJava("\\r").equals("\r"));
    assertTrue(UtilMDE.unescapeNonJava("split\\nlines")
               .equals("split\nlines"));
    assertTrue(UtilMDE.unescapeNonJava("\\\\\\n").equals("\\\n")); // not lineSep
    assertTrue(UtilMDE.unescapeNonJava("\\n\\r").equals("\n\r")); // not lineSep
    assertTrue(UtilMDE.unescapeNonJava("\\\\\\n\\r\\\"").equals("\\\n\r\""));
    assertTrue(UtilMDE.unescapeNonJava("\\\\relax").equals("\\relax"));
    assertTrue(UtilMDE.unescapeNonJava("\\\"hello\\\"").equals("\"hello\""));
    assertTrue(UtilMDE.unescapeNonJava("\\\"hello\\\" \\\"world\\\"")
               .equals("\"hello\" \"world\""));
    assertTrue(UtilMDE.unescapeNonJava("\\").equals("\\"));
    assertTrue(UtilMDE.unescapeNonJava("foo\\").equals("foo\\"));
    assertTrue(UtilMDE.unescapeNonJava("\\*abc").equals("*abc"));
    // Should add more tests here.

    // Unfortunately, there isn't yet a unescapeNonASCII function.
    // If implemented, it should have the following behavior:
    // assertTrue(UtilMDE.unescapeNonASCII("\\115").equals("M"));
    // assertTrue(UtilMDE.unescapeNonASCII("\\115\\111\\124").equals("MIT"));

    // public static String removeWhitespaceAround(String arg, String delimiter)
    // public static String removeWhitespaceAfter(String arg, String delimiter)
    // public static String removeWhitespaceBefore(String arg, String delimiter)

    assertTrue(UtilMDE.removeWhitespaceBefore("a,b", ",").equals("a,b"));
    assertTrue(UtilMDE.removeWhitespaceBefore("a, b", ",").equals("a, b"));
    assertTrue(UtilMDE.removeWhitespaceBefore("a ,b", ",").equals("a,b"));
    assertTrue(UtilMDE.removeWhitespaceBefore("a , b", ",").equals("a, b"));
    assertTrue(UtilMDE.removeWhitespaceBefore("ab=>cd", "=>").equals("ab=>cd"));
    assertTrue(UtilMDE.removeWhitespaceBefore("ab=> cd", "=>").equals("ab=> cd"));
    assertTrue(UtilMDE.removeWhitespaceBefore("ab =>cd", "=>").equals("ab=>cd"));
    assertTrue(UtilMDE.removeWhitespaceBefore("ab => cd", "=>").equals("ab=> cd"));
    assertTrue(UtilMDE.removeWhitespaceBefore("123cd", "123").equals("123cd"));
    assertTrue(UtilMDE.removeWhitespaceBefore(" 123 cd", "123").equals("123 cd"));
    assertTrue(UtilMDE.removeWhitespaceBefore(" 123cd", "123").equals("123cd"));
    assertTrue(UtilMDE.removeWhitespaceBefore("123 cd", "123").equals("123 cd"));
    assertTrue(UtilMDE.removeWhitespaceBefore("cd123", "123").equals("cd123"));
    assertTrue(UtilMDE.removeWhitespaceBefore("cd 123 ", "123").equals("cd123 "));
    assertTrue(UtilMDE.removeWhitespaceBefore("cd123 ", "123").equals("cd123 "));
    assertTrue(UtilMDE.removeWhitespaceBefore("cd 123", "123").equals("cd123"));

    assertTrue(UtilMDE.removeWhitespaceAfter("a,b", ",").equals("a,b"));
    assertTrue(UtilMDE.removeWhitespaceAfter("a, b", ",").equals("a,b"));
    assertTrue(UtilMDE.removeWhitespaceAfter("a ,b", ",").equals("a ,b"));
    assertTrue(UtilMDE.removeWhitespaceAfter("a , b", ",").equals("a ,b"));
    assertTrue(UtilMDE.removeWhitespaceAfter("ab=>cd", "=>").equals("ab=>cd"));
    assertTrue(UtilMDE.removeWhitespaceAfter("ab=> cd", "=>").equals("ab=>cd"));
    assertTrue(UtilMDE.removeWhitespaceAfter("ab =>cd", "=>").equals("ab =>cd"));
    assertTrue(UtilMDE.removeWhitespaceAfter("ab => cd", "=>").equals("ab =>cd"));
    assertTrue(UtilMDE.removeWhitespaceAfter("123cd", "123").equals("123cd"));
    assertTrue(UtilMDE.removeWhitespaceAfter(" 123 cd", "123").equals(" 123cd"));
    assertTrue(UtilMDE.removeWhitespaceAfter(" 123cd", "123").equals(" 123cd"));
    assertTrue(UtilMDE.removeWhitespaceAfter("123 cd", "123").equals("123cd"));
    assertTrue(UtilMDE.removeWhitespaceAfter("cd123", "123").equals("cd123"));
    assertTrue(UtilMDE.removeWhitespaceAfter("cd 123 ", "123").equals("cd 123"));
    assertTrue(UtilMDE.removeWhitespaceAfter("cd123 ", "123").equals("cd123"));
    assertTrue(UtilMDE.removeWhitespaceAfter("cd 123", "123").equals("cd 123"));

    assertTrue(UtilMDE.removeWhitespaceAround("a,b", ",").equals("a,b"));
    assertTrue(UtilMDE.removeWhitespaceAround("a, b", ",").equals("a,b"));
    assertTrue(UtilMDE.removeWhitespaceAround("a ,b", ",").equals("a,b"));
    assertTrue(UtilMDE.removeWhitespaceAround("a , b", ",").equals("a,b"));
    assertTrue(UtilMDE.removeWhitespaceAround("ab=>cd", "=>").equals("ab=>cd"));
    assertTrue(UtilMDE.removeWhitespaceAround("ab=> cd", "=>").equals("ab=>cd"));
    assertTrue(UtilMDE.removeWhitespaceAround("ab =>cd", "=>").equals("ab=>cd"));
    assertTrue(UtilMDE.removeWhitespaceAround("ab => cd", "=>").equals("ab=>cd"));
    assertTrue(UtilMDE.removeWhitespaceAround("123cd", "123").equals("123cd"));
    assertTrue(UtilMDE.removeWhitespaceAround(" 123 cd", "123").equals("123cd"));
    assertTrue(UtilMDE.removeWhitespaceAround(" 123cd", "123").equals("123cd"));
    assertTrue(UtilMDE.removeWhitespaceAround("123 cd", "123").equals("123cd"));
    assertTrue(UtilMDE.removeWhitespaceAround("cd123", "123").equals("cd123"));
    assertTrue(UtilMDE.removeWhitespaceAround("cd 123 ", "123").equals("cd123"));
    assertTrue(UtilMDE.removeWhitespaceAround("cd123 ", "123").equals("cd123"));
    assertTrue(UtilMDE.removeWhitespaceAround("cd 123", "123").equals("cd123"));

    // public static String nplural(int n, String noun)

    assertTrue(UtilMDE.nplural(0, "fuss").equals("0 fusses"));
    assertTrue(UtilMDE.nplural(1, "fuss").equals("1 fuss"));
    assertTrue(UtilMDE.nplural(2, "fuss").equals("2 fusses"));
    assertTrue(UtilMDE.nplural(0, "fox").equals("0 foxes"));
    assertTrue(UtilMDE.nplural(1, "fox").equals("1 fox"));
    assertTrue(UtilMDE.nplural(2, "fox").equals("2 foxes"));
    assertTrue(UtilMDE.nplural(0, "fish").equals("0 fishes"));
    assertTrue(UtilMDE.nplural(1, "fish").equals("1 fish"));
    assertTrue(UtilMDE.nplural(2, "fish").equals("2 fishes"));
    assertTrue(UtilMDE.nplural(0, "fletch").equals("0 fletches"));
    assertTrue(UtilMDE.nplural(1, "fletch").equals("1 fletch"));
    assertTrue(UtilMDE.nplural(2, "fletch").equals("2 fletches"));
    assertTrue(UtilMDE.nplural(0, "fund").equals("0 funds"));
    assertTrue(UtilMDE.nplural(1, "fund").equals("1 fund"));
    assertTrue(UtilMDE.nplural(2, "fund").equals("2 funds"));

    // public static String rpad(String s, int length)
    // public static String rpad(int num, int length)
    // public static String rpad(double num, int length)

    assertTrue(UtilMDE.rpad("", 5).equals("     "));
    assertTrue(UtilMDE.rpad("abcd", 5).equals("abcd "));
    assertTrue(UtilMDE.rpad("abcde", 5).equals("abcde"));
    assertTrue(UtilMDE.rpad("abcdef", 5).equals("abcde"));
    assertTrue(UtilMDE.rpad("abcdefghij", 5).equals("abcde"));
    assertTrue(UtilMDE.rpad(10, 5).equals("10   "));
    assertTrue(UtilMDE.rpad(3.14, 5).equals("3.14 "));

    // public static class NullableStringComparator
    //   public int compare(Object o1, Object o2)

    // public static int count(String s, int ch)
    // public static int count(String s, String sub)

    assertTrue(UtilMDE.count("abcde", 'a') == 1);
    assertTrue(UtilMDE.count("abcde", 'c') == 1);
    assertTrue(UtilMDE.count("abcde", 'e') == 1);
    assertTrue(UtilMDE.count("abcde", 'z') == 0);
    assertTrue(UtilMDE.count("abacadaea", 'a') == 5);
    assertTrue(UtilMDE.count("aaadaea", 'a') == 5);
    assertTrue(UtilMDE.count("daeaaa", 'a') == 4);

    // This will be easy to write tests for, when I get around to it.
    // public static Vector tokens(String str, String delim, boolean returnTokens)
    // public static Vector tokens(String str, String delim)
    // public static Vector tokens(String str)

    // public static List sortList (List l, Comparator c)
    // public static <T> List<T> removeDuplicates(List<T> l) {

    List<Integer> l123 = new ArrayList<Integer>();
    l123.add(1); l123.add(2); l123.add(3);
    List<Integer> l123123 = new ArrayList<Integer>();
    l123123.add(1); l123123.add(2); l123123.add(3); l123123.add(1); l123123.add(2); l123123.add(3);
    List<Integer> l12223 = new ArrayList<Integer>();
    l12223.add(1); l12223.add(2); l12223.add(2); l12223.add(2); l12223.add(3);
    List<Integer> l1123 = new ArrayList<Integer>();
    l1123.add(1); l1123.add(1); l1123.add(2); l1123.add(3);
    List<Integer> l1233 = new ArrayList<Integer>();
    l1233.add(1); l1233.add(1); l1233.add(2); l1233.add(3);

    assertTrue(UtilMDE.removeDuplicates(l123).equals(l123));
    assertTrue(UtilMDE.removeDuplicates(l123123).equals(l123));
    assertTrue(UtilMDE.removeDuplicates(l12223).equals(l123));
    assertTrue(UtilMDE.removeDuplicates(l1123).equals(l123));
    assertTrue(UtilMDE.removeDuplicates(l1233).equals(l123));

    // This is tested by the tokens methods.
    // public static Vector makeVector(Enumeration e)
  }

  public static void testTestUtilMDE() {
    int[] a = new int[] { 3,4,5 };
    assert_arrays_equals(int_iterator_array(int_array_iterator(a)), a);
  }

  public static void testWeakHasherMap() {
  }

  /**
   * These tests could be much more thorough.  Basically all that is tested
   * is that identity is used rather than a normal hash.  The tests will
   * fail however, if WeakHashMap is swapped for WeakIdentityHashMap.
   */
  public static void testWeakIdentityHashMap() {

    String s1 = "one";
    String s2 = "two";
    String s3 = "three";

    WeakIdentityHashMap<String,Integer> m
      = new WeakIdentityHashMap<String,Integer>();
    // WeakHashMap<String,Integer> m = new WeakHashMap<String,Integer>();

    m.put (s1, 1);
    m.put (s2, 2);
    m.put (s3, 3);

    String s1a = new String(s1);
    String s2a = new String(s2);
    String s3a = new String(s3);

    m.put (s1a, 1);
    m.put (s2a, 2);
    m.put (s3a, 3);

    Assert.assertTrue (m.get(s1) == 1);
    Assert.assertTrue (m.get(s2) == 2);
    Assert.assertTrue (m.get(s3) == 3);
    Assert.assertTrue (m.get(s1a) == 1);
    Assert.assertTrue (m.get(s2a) == 2);
    Assert.assertTrue (m.get(s3a) == 3);

    m.remove (s1);
    m.remove (s2);
    m.remove (s3);
    Assert.assertTrue (m.get(s1) == null);
    Assert.assertTrue (m.get(s2) == null);
    Assert.assertTrue (m.get(s3) == null);
    Assert.assertTrue (m.get(s1a) == 1);
    Assert.assertTrue (m.get(s2a) == 2);
    Assert.assertTrue (m.get(s3a) == 3);

  }

  /**
   * Tests whether CountingPrintWriter
   * counts the bytes printed, written for
   * different types (boolean, int, float etc.).
   **/
  public static void testCountingPrintWriter() {
    CountingPrintWriter c1 = new CountingPrintWriter(new CharArrayWriter());
    c1.print("a");
    Assert.assertTrue(c1.getNumberOfPrintedBytes() == 1);
    c1.print(1);
    Assert.assertTrue(c1.getNumberOfPrintedBytes() == 2);
    c1.print(true);
    Assert.assertTrue(c1.getNumberOfPrintedBytes() == 6);
    c1.print(1.00);
    Assert.assertTrue(c1.getNumberOfPrintedBytes() == 9);
    c1.write("a");
    c1.write("a");
    Assert.assertTrue(c1.getNumberOfPrintedBytes() == 9);
    Assert.assertTrue(c1.getNumberOfWrittenBytes() == 22);
    Assert.assertTrue(c1.getNumberOfPrintedChars() == 9);
    c1.println("foo");
    int ls_len = System.getProperty("line.separator").length();
    Assert.assertTrue(c1.getNumberOfPrintedBytes() == (12 + ls_len));
    Assert.assertTrue(c1.getNumberOfWrittenBytes() == (28));
    Assert.assertTrue(c1.getNumberOfPrintedChars() == (12 + ls_len));
  }


  /**
   * Test the intering of subsequences as triples of the original
   * sequence, the start and the end indices.
   **/
  public static void testSequenceAndIndices() {
    int[] a1 = Intern.intern(new int[] {1, 2, 3, 4, 5, 6, 7});
    int[] a2 = Intern.intern(new int[] {1, 2, 3, 4, 5, 6, 7});
    int[] a3 = Intern.intern(new int[] {2, 3, 4, 5, 6, 7});
    int i = 2;
    int j = 4;
    int k = 5;

    int[] s1 = Intern.internSubsequence (a1, i, j);
    int[] s2 = Intern.internSubsequence (a2, i, j);
    int[] s3 = Intern.internSubsequence (a1, j, k);
    int[] s4 = Intern.internSubsequence (a1, j, k);
    int[] s5 = Intern.internSubsequence (a3, j-1, k-1);

    Assert.assertTrue (a1 == a2);
    Assert.assertTrue (s1 == s2);
    Assert.assertTrue (s3 == s4);
    Assert.assertTrue (s3 == s5);
    Assert.assertTrue (ArraysMDE.isSubarray(s1, ArraysMDE.subarray (a1, i, j-i), 0));
    Assert.assertTrue (ArraysMDE.isSubarray(ArraysMDE.subarray (a1, i, j-i), s1, 0));

    long[] l1 = Intern.intern(new long[] {1, 2, 3, 4, 5, 6});
    Assert.assertTrue (l1 == Intern.internSubsequence (l1, 0, l1.length));
  }

  /**
   * Test the comparison, indexof, and set equivalence calls in fuzzy
   * float.
   */
  public static void testFuzzyFloat() {

    FuzzyFloat  ff = new FuzzyFloat (0.0001);
    double      offset   = 0.00007;
    double      offhigh  = 1 + offset;
    double      offlow   = 1 - offset;
    double      offhigh2 = 1 + 2*offset;
    double      offlow2  = 1 - 2*offset;

    //test equality for a variety of postive and negative numbers
    for (double d = -20000; d < 20000; d += 1000.36) {
      assertTrue (ff.eq (d, d * offhigh));
      assertTrue (ff.eq (d, d * offlow));
      assertTrue (!ff.eq (d, d * offhigh2));
      assertTrue (!ff.eq (d, d * offlow2));
      assertTrue (!ff.ne (d, d * offhigh));
      assertTrue (!ff.ne (d, d * offlow));
      assertTrue (ff.ne (d, d * offhigh2));
      assertTrue (ff.ne (d, d * offlow2));
    }

    //make sure nothing is equal to zero
    assertTrue (ff.eq (0, Double.MIN_VALUE));
    assertTrue (ff.eq (0, -Double.MIN_VALUE));
    assertTrue (!ff.ne (0, Double.MIN_VALUE));
    assertTrue (!ff.ne (0, -Double.MIN_VALUE));

    //make sure that 0 equals 0
    assertTrue (ff.eq (0, 0));
    assertTrue (!ff.ne (0, 0));

    //make sure that NaNs are not equal
    assertTrue (!ff.eq (Double.NaN, Double.NaN));

    //make sure that various unusual values are equal
    assertTrue (ff.eq (Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    assertTrue (ff.eq (Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));

    //rudimentary checks on the comparison operators (since they all just
    //use eq and ne anyway)
    {
      double d = 2563.789;
      assertTrue (!ff.gt (d, d * offlow));
      assertTrue (!ff.lt (d, d * offhigh));
      assertTrue (ff.gt (d, d * offlow2));
      assertTrue (ff.lt (d, d * offhigh2));
      assertTrue (ff.gte (d, d * offhigh));
      assertTrue (ff.lte (d, d * offlow));
      assertTrue (!ff.gte (d, d * offhigh2));
      assertTrue (!ff.lte (d, d * offlow2));
    }

    // public int indexOf (double[] a, double elt)
    {
      double[] a = new double[10];
      for (int i=0; i<a.length; i++)
        a[i] = i;
      double[] a_copy = a.clone();
      assertTrue(ff.indexOf(a, -1) == -1);
      assertTrue(ff.indexOf(a, 0) == 0);
      assertTrue(ff.indexOf(a, 7) == 7);
      assertTrue(ff.indexOf(a, 9) == 9);
      assertTrue(ff.indexOf(a, 10) == -1);
      assertTrue(ff.indexOf(a, 20) == -1);
      assertTrue(ff.indexOf(a, Double.MIN_VALUE) == 0);
      assertTrue(ff.indexOf(a, 7 * offhigh) == 7);
      assertTrue(ff.indexOf(a, 9 * offlow) == 9);
      assertTrue(ff.indexOf(a, 7 * offhigh2) == -1);
      assertTrue(ff.indexOf(a, 9 * offlow2) == -1);
      assert_arrays_equals (a, a_copy);
    }

    // public int indexOf (double[] a, double[] sub)
    {
      double[] a = new double[10];
      for (int i=0; i<a.length; i++)
        a[i] = i;
      double[] b = new double[] { };
      double[] c = new double[] { a[0], a[1], a[2] };
      double[] d = new double[] { a[1], a[2] };
      double[] e = new double[] { a[2], a[3], a[4], a[5] };
      double[] f = new double[] { a[7], a[8], a[9] };
      double[] g = new double[] { a[7], 22, a[9] };
      double[] h = new double[] { a[7], a[8], a[9], 10 };

      assertTrue(ff.indexOf(a, b) == 0);
      assertTrue(ff.indexOf(a, c) == 0);
      assertTrue(ff.indexOf(a, d) == 1);
      assertTrue(ff.indexOf(a, e) == 2);
      assertTrue(ff.indexOf(a, f) == 7);
      assertTrue(ff.indexOf(a, g) == -1);
      assertTrue(ff.indexOf(a, h) == -1);
    }
    {
      double[] a = new double[10];
      for (int i=0; i<a.length; i++)
        a[i] = i;
      double[] b = new double[] { };
      double[] c = new double[] { a[0] *offlow, a[1]*offhigh, a[2]*offlow };
      double[] d = new double[] { a[1]*offhigh, a[2]*offlow };
      double[] e = new double[] { a[2], a[3], a[4]*offlow, a[5]*offhigh };
      double[] f = new double[] { a[7], a[8]*offlow, a[9]*offhigh };
      double[] g = new double[] { a[7], 22, a[9] };
      double[] h = new double[] { a[7], a[8], a[9], 10 };
      double[] a_copy = a.clone();
      double[] b_copy = b.clone();
      double[] c_copy = c.clone();
      double[] d_copy = d.clone();
      double[] e_copy = e.clone();
      double[] f_copy = f.clone();
      double[] g_copy = g.clone();
      double[] h_copy = h.clone();

      assertTrue(ff.indexOf(a, b) == 0);
      assertTrue(ff.indexOf(a, c) == 0);
      assertTrue(ff.indexOf(a, d) == 1);
      assertTrue(ff.indexOf(a, e) == 2);
      assertTrue(ff.indexOf(a, f) == 7);
      assertTrue(ff.indexOf(a, g) == -1);
      assertTrue(ff.indexOf(a, h) == -1);

      assert_arrays_equals (a, a_copy);
      assert_arrays_equals (b, b_copy);
      assert_arrays_equals (c, c_copy);
      assert_arrays_equals (d, d_copy);
      assert_arrays_equals (e, e_copy);
      assert_arrays_equals (f, f_copy);
      assert_arrays_equals (g, g_copy);
      assert_arrays_equals (h, h_copy);
    }

    // public boolean isElemMatch (double[] a1, double[] a2)
    {
      double[] f1 = new double[10];
      double[] f2 = new double[20];

      for (int j = 0; j < 10; j++) {

        //start two arrays out exactly equal
        for (int i = 0; i < f1.length; i++) {
          f1[i] = j + i * 10;
          f2[i] = j + i * 10;
          }

        //fill out the second half of f2 with dup of f1
        for (int i = 10; i < f2.length; i++) {
          f2[i] = j + (i - 10) * 10;
        }

        //make two elements off just a little
        f2[7] = f2[7] * (1 + offset);
        f2[8] = f2[8] * (1 - offset);

        //test with each array the bigger one
        if ((j % 2) == 0) {
          assertTrue (ff.isElemMatch (f1, f2));
        } else {
          assertTrue (ff.isElemMatch (f2, f1));
        }
      }
      for (int j = 0; j < 200; j++) {

        //start two arrays out exactly equal
        for (int i = 0; i < f1.length; i++) {
          f1[i] = j + i * 10;
          f2[i] = j + i * 10;
          }

        //fill out the second half of f2 with dup of f1
        for (int i = 10; i < f2.length; i++) {
          f2[i] = j + (i - 10) * 10;
        }

        //make two elements off just a little
        f2[7] = f2[7] * (1 + 2*offset);
        f2[8] = f2[8] * (1 - 2*offset);

        //test with each array the bigger one
        double[] f1_copy = f1.clone();
        double[] f2_copy = f2.clone();
        if ((j % 2) == 0) {
          assertTrue (!ff.isElemMatch (f1, f2));
        } else {
          assertTrue (!ff.isElemMatch (f2, f1));
        }
        assert_arrays_equals (f1, f1_copy);
        assert_arrays_equals (f2, f2_copy);
      }
    }
    {
      double[] a = new double[] {2, 1, 0};
      double[] b = new double[] { };
      double[] c = new double[] {1, 1, 1, 1};
      double[] d = new double[] {1};
      assertTrue (!ff.isElemMatch (a, b));
      assertTrue (!ff.isElemMatch (b, a));
      assertTrue (ff.isElemMatch (c, d));
      assertTrue (ff.isElemMatch (d, c));
      assertTrue (ff.isElemMatch (b, b));
    }

    // public class DoubleArrayComparatorLexical implements Comparator
    // public int compare(Object o1, Object o2)
    {
      Comparator<double[]> comparator = ff.new DoubleArrayComparatorLexical();
      double[] a0 = new double[] { };
      double[] a1 = new double[] { };
      double[] a2 = new double[] { 0,1,2,3 };
      double[] a3 = new double[] { 0,1,2,3,0 };
      double[] a4 = new double[] { 0,1,2,3,4 };
      double[] a5 = new double[] { 0,1,2,3,4 };
      double[] a6 = new double[] { 0,1,5,3,4 };
      double[] a7 = new double[] { 1,2,3,4 };
      double[] a0_copy = a0.clone();
      double[] a1_copy = a1.clone();
      double[] a2_copy = a2.clone();
      double[] a3_copy = a3.clone();
      double[] a4_copy = a4.clone();
      double[] a5_copy = a5.clone();
      double[] a6_copy = a6.clone();
      double[] a7_copy = a7.clone();

      assertTrue(comparator.compare(a0, a1) == 0);
      assertTrue(comparator.compare(a1, a0) == 0);
      assertTrue(comparator.compare(a1, a2) < 0);
      assertTrue(comparator.compare(a2, a1) > 0);
      assertTrue(comparator.compare(a2, a3) < 0);
      assertTrue(comparator.compare(a3, a2) > 0);
      assertTrue(comparator.compare(a3, a4) < 0);
      assertTrue(comparator.compare(a4, a3) > 0);
      assertTrue(comparator.compare(a4, a5) == 0);
      assertTrue(comparator.compare(a5, a4) == 0);
      assertTrue(comparator.compare(a5, a6) < 0);
      assertTrue(comparator.compare(a6, a5) > 0);
      assertTrue(comparator.compare(a6, a7) < 0);
      assertTrue(comparator.compare(a7, a6) > 0);
      assertTrue(comparator.compare(a1, a4) < 0);
      assertTrue(comparator.compare(a4, a1) > 0);
      assertTrue(comparator.compare(a2, a4) < 0);
      assertTrue(comparator.compare(a4, a2) > 0);
      assertTrue(comparator.compare(a6, a4) > 0);
      assertTrue(comparator.compare(a4, a6) < 0);
      assertTrue(comparator.compare(a7, a4) > 0);
      assertTrue(comparator.compare(a4, a7) < 0);

      assert_arrays_equals (a0, a0_copy);
      assert_arrays_equals (a1, a1_copy);
      assert_arrays_equals (a2, a2_copy);
      assert_arrays_equals (a3, a3_copy);
      assert_arrays_equals (a4, a4_copy);
      assert_arrays_equals (a5, a5_copy);
      assert_arrays_equals (a6, a6_copy);
      assert_arrays_equals (a7, a7_copy);
    }

    // public boolean FuzzyFloat.isSubset (double[] a1, double[] a2)
    {
      double[] f1 = new double[10];
      double[] f2 = new double[20];

      for (int j = 0; j < f2.length; j++)
        f2[j] = j;
      for (int i = 0; i < f2.length - f1.length; i++) {

        //fill up f1 with elements of f2
        for (int j = 0; j < f1.length; j++)
          f1[j] = f2[i+j];

        f1[5] = f2[i] * offhigh;

        double[] f1_copy = f1.clone();
        double[] f2_copy = f2.clone();

        assertTrue (ff.isSubset (f1, f2));
        assert_arrays_equals (f1, f1_copy);
        assert_arrays_equals (f2, f2_copy);
      }

      double [] a1 = new double [] {1, 5, 10};
      double [] a2 = new double [] {};
      double [] a3 = new double [] {1};
      double [] a4 = new double [] {10};
      double [] a5 = new double [] {1, 10, 15, 20};
      double [] a6 = new double [] {10, 10, 10, 10, 10, 1};

      assertTrue (ff.isSubset (a2, a1));
      assertTrue (!ff.isSubset (a1, a2));
      assertTrue (!ff.isSubset (a1, a5));
      assertTrue (ff.isSubset (a3, a1));
      assertTrue (ff.isSubset (a4, a1));
      assertTrue (ff.isSubset (a6, a1));
      assertTrue (!ff.isSubset (a1, a6));
    }

  }

  /**
   * Tests UtilMDE create_combinations routines.
   */

  public static void test_create_combinations() {

    // public static List create_combinations (int dims, int start, List objs)
    Object a = new Object();
    Object b = new Object();
    Object c = new Object();
    List a_list = Arrays.asList (new Object[] {a});
    List b_list = Arrays.asList (new Object[] {b});
    List c_list = Arrays.asList (new Object[] {c});
    List aa = Arrays.asList (new Object[] {a, a});
    List bb = Arrays.asList (new Object[] {b, b});
    List cc = Arrays.asList (new Object[] {c, c});
    List ab = Arrays.asList (new Object[] {a, b});
    List ac = Arrays.asList (new Object[] {a, c});
    List bc = Arrays.asList (new Object[] {b, c});

    List<Object> abc = Arrays.asList (new Object[] {a,b,c});
    List<List<Object>> combo1 = UtilMDE.create_combinations (1, 0, abc);
    assertTrue (combo1.size() == 3);
    assertTrue (combo1.contains (a_list));
    assertTrue (combo1.contains (b_list));
    assertTrue (combo1.contains (c_list));

    List<List<Object>> combo2 = UtilMDE.create_combinations (2, 0, abc);
    assertTrue (combo2.size() == 6);
    assertTrue (combo2.contains (aa));
    assertTrue (combo2.contains (ab));
    assertTrue (combo2.contains (ac));
    assertTrue (combo2.contains (bb));
    assertTrue (combo2.contains (bc));
    assertTrue (combo2.contains (cc));

    // public static List create_combinations (int arity, int start, int cnt)
    Integer i0 = new Integer(0);
    Integer i1 = new Integer(1);
    Integer i2 = new Integer(2);

    List combo3 = UtilMDE.create_combinations (1, 0, 2);
    assertTrue (combo3.size() == 3);
    assertTrue (combo3.contains (Arrays.asList (new Integer [] {i0})));
    assertTrue (combo3.contains (Arrays.asList (new Integer [] {i1})));
    assertTrue (combo3.contains (Arrays.asList (new Integer [] {i2})));

    List combo4 = UtilMDE.create_combinations (2, 0, 2);
    assertTrue (combo4.size() == 6);
    assertTrue (combo4.contains (Arrays.asList (new Integer[] { i0, i0})));
    assertTrue (combo4.contains (Arrays.asList (new Integer[] { i0, i1})));
    assertTrue (combo4.contains (Arrays.asList (new Integer[] { i0, i2})));
    assertTrue (combo4.contains (Arrays.asList (new Integer[] { i1, i1})));
    assertTrue (combo4.contains (Arrays.asList (new Integer[] { i1, i2})));
    assertTrue (combo4.contains (Arrays.asList (new Integer[] { i2, i2})));

  }

  public static void test_unqualified_name () {

    assertTrue (UtilMDE.unqualified_name("java.lang.String").equals("String"));
    assertTrue (UtilMDE.unqualified_name ("String").equals ("String"));

  }

  /**
   * Test class for Options testing
   */
  public static class TestOptions {

    @Option ("list of patterns") List<Pattern> lp = new ArrayList<Pattern>();
    @Option ("-a <filename> argument 1") String arg1 = "/tmp/foobar";
    @Option ("argument 2") String arg2;
    @Option ("-d double value") double temperature;
    @Option ("-f the input file") File input_file;
    @Option ("-b boolean") boolean bool;
    @Option ("-i Integer") Integer integer_reference;
    @Option ("list of doubles") List<Double> ld = new ArrayList<Double>();
  }

  /**
   * Test command line option parsing (Options)
   */
  public static void testOptions() throws ArgException {

    TestOptions t = new TestOptions();
    Options options = new Options ("test", t);
    options.parse (new String[] { "--lp=foo",
                                  "--lp", "bar",
                                  "-i", "24",
                                  "-d=37.8",
                                  "-b",
                                  "-b=false",
                                  "--ld", "34.6",
                                  "--ld", "17.8",
                                 });
    assertEquals (t.lp.get(0).toString(), "foo");
    assertEquals (t.lp.get(1).toString(), "bar");
    assertEquals (t.integer_reference.intValue(), 24);
    assertEquals (t.temperature, 37.8);
    assertEquals (t.bool, false);
    assertEquals (t.ld.get(0).doubleValue(), 34.6);
    assertEquals (t.ld.get(1).doubleValue(), 17.8);
  }

  public static void testSplitLines() {

    String str = "one\ntwo\n\rthree\r\nfour\rfive\n\n\nsix\r\n\r\n\r\n";
    String[] sa = UtilMDE.splitLines (str);
    // for (String s : sa)
    //   System.out.printf ("'%s'\n", s);
    assertEquals (sa.length, 11);
    assertEquals (sa[0], "one");
    assertEquals (sa[1], "two");
    assertEquals (sa[2], "three");
    assertEquals (sa[3], "four");
    assertEquals (sa[4], "five");
    assertEquals (sa[5], "");
    assertEquals (sa[6], "");
    assertEquals (sa[7], "six");
    assertEquals (sa[8], "");
    assertEquals (sa[9], "");
    assertEquals (sa[10], "");
  }
}
