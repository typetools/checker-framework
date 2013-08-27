package utilMDE;

import checkers.quals.Interned;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Utilities for interning objects.
 **/
public final class Intern {
  private Intern() { throw new Error("do not instantiate"); }

  ///////////////////////////////////////////////////////////////////////////
  /// Strings
  ///

  /**
   * Replace each element of the array by its interned version.
   * @see String#intern
   **/
  public static void internStrings(String[] a) {
    for (int i=0; i<a.length; i++)
      if (a[i] != null)
        a[i] = a[i].intern();
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Testing interning
  ///

  /**
   * Return true if the argument is interned (is canonical among all
   * objects equal to itself).
   **/
  @SuppressWarnings("interned")
  public static boolean isInterned(Object value) {
    if (value == null) {
      // nothing to do
      return true;
    } else if (value instanceof String) {
      return (value == ((String) value).intern());
    } else if (value instanceof String[]) {
      return (value == intern((String[]) value));
    } else if (value instanceof Integer) {
      return (value == intern((Integer) value));
    } else if (value instanceof Long) {
      return (value == intern((Long) value));
    } else if (value instanceof int[]) {
      return (value == intern((int[]) value));
    } else if (value instanceof long[]) {
      return (value == intern((long[]) value));
    } else if (value instanceof Double) {
      return (value == intern((Double) value));
    } else if (value instanceof double[]) {
      return (value == intern((double[]) value));
    } else if (value instanceof Object[]) {
      return (value == intern((Object[]) value));
    } else {
      // Nothing to do, because we don't intern other types.
      // System.out.println("What type? " + value.getClass().getName());
      return true;
    }
  }


  ///////////////////////////////////////////////////////////////////////////
  /// Interning objects
  ///

  /**
   * Hasher object which hashes and compares Integers.
   * This is the obvious implementation that uses intValue() for the hashCode.
   * @see Hasher
   **/
  private static final class IntegerHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return a1.equals(a2);
    }
    public int hashCode(Object o) {
      Integer i = (Integer) o;
      return i.intValue();
    }
  }

  /**
   * Hasher object which hashes and compares Longs.
   * This is the obvious implementation that uses intValue() for the hashCode.
   * @see Hasher
   **/
  private static final class LongHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return a1.equals(a2);
    }
    public int hashCode(Object o) {
      Long i = (Long) o;
      return i.intValue();
    }
  }

  /**
   * Hasher object which hashes and compares int[] objects according
   * to their contents.
   * @see Hasher
   * @see java.util.Arrays#equals(int[], int[])
   **/
  private static final class IntArrayHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return java.util.Arrays.equals((int[])a1, (int[])a2);
    }
    public int hashCode(Object o) {
      int[] a = (int[])o;
      int result = 0;
      for (int i=0; i<a.length; i++) {
        result = result * FACTOR + a[i];
      }
      return result;
    }
  }

  /**
   * Hasher object which hashes and compares long[] objects according
   * to their contents.
   * @see Hasher
   * @see java.util.Arrays#equals (long[], long[])
   **/
  private static final class LongArrayHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return java.util.Arrays.equals((long[])a1, (long[])a2);
    }
    public int hashCode(Object o) {
      long[] a = (long[])o;
      long result = 0;
      for (int i=0; i<a.length; i++) {
        result = result * FACTOR + a[i];
      }
      return (int) (result % Integer.MAX_VALUE);
    }
  }

  private static final int FACTOR = 23;
  // private static final double DOUBLE_FACTOR = 65537;
  private static final double DOUBLE_FACTOR = 263;

  /**
   * Hasher object which hashes and compares Doubles.
   * @see Hasher
   **/
  private static final class DoubleHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return a1.equals(a2);
    }
    public int hashCode(Object o) {
      Double d = (Double) o;
      // Could add "... % Integer.MAX_VALUE" here; is that good to do?
      long result = Math.round(d.doubleValue() * DOUBLE_FACTOR);
      return (int) (result % Integer.MAX_VALUE);
    }
  }

  /**
   * Hasher object which hashes and compares double[] objects according
   * to their contents.
   * @see Hasher
   * @see java.util.Arrays#equals(Object[],Object[])
   **/
  private static final class DoubleArrayHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      // "java.util.Arrays.equals" considers +0.0 != -0.0.
      // Also, it gives inconsistent results (on different JVMs/classpaths?).
      // return java.util.Arrays.equals((double[])a1, (double[])a2);
      double[] da1 = (double[])a1;
      double[] da2 = (double[])a2;
      if (da1.length != da2.length)
        return false;
      for (int i=0; i<da1.length; i++) {
        if (! ((da1[i] == da2[i])
               || (Double.isNaN(da1[i]) && Double.isNaN(da2[i])))) {
          return false;
        }
      }
      return true;
    }
    public int hashCode(Object o) {
      double[] a = (double[])o;
      double running = 0;
      for (int i=0; i<a.length; i++) {
        double elt = (Double.isNaN(a[i]) ? 0.0 : a[i]);
        running = running * FACTOR + elt * DOUBLE_FACTOR;
      }
      // Could add "... % Integer.MAX_VALUE" here; is that good to do?
      long result = Math.round(running);
      return (int) (result % Integer.MAX_VALUE);
    }
  }

  /**
   * Hasher object which hashes and compares String[] objects according
   * to their contents.
   * @see Hasher
   * java.util.Arrays.equals
   **/
  private static final class StringArrayHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return java.util.Arrays.equals((String[])a1, (String[])a2);
    }
    public int hashCode(Object o) {
      String[] a = (String[])o;
      int result = 0;
      for (int i=0; i<a.length; i++) {
        int a_hashcode = (a[i] == null) ? 0 : a[i].hashCode();
        result = result * FACTOR + a_hashcode;
      }
      return result;
    }
  }

  /**
   * Hasher object which hashes and compares Object[] objects according
   * to their contents.
   * @see Hasher
   * @see java.util.Arrays#equals(Object[], Object[])
   **/
  private static final class ObjectArrayHasher implements Hasher {
    public boolean equals(Object a1, Object a2) {
      return java.util.Arrays.equals((Object[])a1, (Object[])a2);
    }
    public int hashCode(Object o) {
      Object[] a = (Object[])o;
      int result = 0;
      for (int i=0; i<a.length; i++) {
        int a_hashcode = (a[i] == null) ? 0 : a[i].hashCode();
        result = result * FACTOR + a_hashcode;
      }
      return result;
    }
  }

  // Map from an ArrayWrapper to the array (I don't need to map to a
  // WeakReference because the array isn't the key of the WeakHashMap).

  private static WeakHasherMap</*@Interned*/ Integer,WeakReference</*@Interned*/ Integer>> internedIntegers;
  private static WeakHasherMap</*@Interned*/ Long,WeakReference</*@Interned*/ Long>> internedLongs;
  private static WeakHasherMap</*@Interned*/ int[],WeakReference</*@Interned*/ int[]>> internedIntArrays;
  private static WeakHasherMap</*@Interned*/ long[],WeakReference</*@Interned*/ long[]>> internedLongArrays;
  private static WeakHasherMap</*@Interned*/ Double,WeakReference</*@Interned*/ Double>> internedDoubles;
  private static /*@Interned*/ Double internedDoubleNaN;
  private static /*@Interned*/ Double internedDoubleZero;
  private static WeakHasherMap</*@Interned*/ double[],WeakReference</*@Interned*/ double[]>> internedDoubleArrays;
  private static WeakHasherMap</*@Interned*/ String[],WeakReference</*@Interned*/ String[]>> internedStringArrays;
  private static WeakHasherMap</*@Interned*/ Object[],WeakReference</*@Interned*/ Object[]>> internedObjectArrays;
  private static WeakHasherMap<SequenceAndIndices</*@Interned*/ int[]>,WeakReference</*@Interned*/ int[]>> internedIntSequenceAndIndices;
  private static WeakHasherMap<SequenceAndIndices</*@Interned*/ long[]>,WeakReference</*@Interned*/ long[]>> internedLongSequenceAndIndices;
  private static WeakHasherMap<SequenceAndIndices</*@Interned*/ double[]>,WeakReference</*@Interned*/ double[]>> internedDoubleSequenceAndIndices;
  private static WeakHasherMap<SequenceAndIndices</*@Interned*/ Object[]>,WeakReference</*@Interned*/ Object[]>> internedObjectSequenceAndIndices;
  private static WeakHasherMap<SequenceAndIndices</*@Interned*/ String[]>,WeakReference</*@Interned*/ String[]>> internedStringSequenceAndIndices;

  static {
    internedIntegers = new WeakHasherMap</*@Interned*/ Integer,WeakReference</*@Interned*/ Integer>>(new IntegerHasher());
    internedLongs = new WeakHasherMap</*@Interned*/ Long,WeakReference</*@Interned*/ Long>>(new LongHasher());
    internedIntArrays = new WeakHasherMap</*@Interned*/ int[],WeakReference</*@Interned*/ int[]>>(new IntArrayHasher());
    internedLongArrays = new WeakHasherMap</*@Interned*/ long[],WeakReference</*@Interned*/ long[]>>(new LongArrayHasher());
    internedDoubles = new WeakHasherMap</*@Interned*/ Double,WeakReference</*@Interned*/ Double>>(new DoubleHasher());
    internedDoubleNaN = new /*@Interned*/ Double(Double.NaN);
    internedDoubleZero = new /*@Interned*/ Double(0);
    internedDoubleArrays = new WeakHasherMap</*@Interned*/ double[],WeakReference</*@Interned*/ double[]>>(new DoubleArrayHasher());
    internedStringArrays = new WeakHasherMap</*@Interned*/ String[],WeakReference</*@Interned*/ String[]>>(new StringArrayHasher());
    internedObjectArrays = new WeakHasherMap</*@Interned*/ Object[],WeakReference</*@Interned*/ Object[]>>(new ObjectArrayHasher());
    internedIntSequenceAndIndices = new WeakHasherMap<SequenceAndIndices</*@Interned*/ int[]>,WeakReference</*@Interned*/ int[]>>(new SequenceAndIndicesHasher<int[]>());
    internedLongSequenceAndIndices = new WeakHasherMap<SequenceAndIndices</*@Interned*/ long[]>,WeakReference</*@Interned*/ long[]>>(new SequenceAndIndicesHasher<long[]>());
    internedDoubleSequenceAndIndices = new WeakHasherMap<SequenceAndIndices</*@Interned*/ double[]>,WeakReference</*@Interned*/ double[]>>(new SequenceAndIndicesHasher<double[]>());
    internedObjectSequenceAndIndices = new WeakHasherMap<SequenceAndIndices</*@Interned*/ Object[]>,WeakReference</*@Interned*/ Object[]>>(new SequenceAndIndicesHasher<Object[]>());
    internedStringSequenceAndIndices = new WeakHasherMap<SequenceAndIndices</*@Interned*/ String[]>,WeakReference</*@Interned*/ String[]>>(new SequenceAndIndicesHasher<String[]>());
  }

  // For testing only
  public static int numIntegers() { return internedIntegers.size(); }
  public static int numLongs() { return internedLongs.size(); }
  public static int numIntArrays() { return internedIntArrays.size(); }
  public static int numLongArrays() { return internedLongArrays.size(); }
  public static int numDoubles() { return internedDoubles.size(); }
  public static int numDoubleArrays() { return internedDoubleArrays.size(); }
  public static int numStringArrays() { return internedStringArrays.size(); }
  public static int numObjectArrays() { return internedObjectArrays.size(); }
  public static Iterator<Integer> integers() { return internedIntegers.keySet().iterator(); }
  public static Iterator<Long> longs() { return internedLongs.keySet().iterator(); }
  public static Iterator<int[]> intArrays() { return internedIntArrays.keySet().iterator(); }
  public static Iterator<long[]> longArrays() { return internedLongArrays.keySet().iterator(); }
  public static Iterator<Double> doubles() { return internedDoubles.keySet().iterator(); }
  public static Iterator<double[]> doubleArrays() { return internedDoubleArrays.keySet().iterator(); }
  public static Iterator<String[]> stringArrays() { return internedStringArrays.keySet().iterator(); }
  public static Iterator<Object[]> objectArrays() { return internedObjectArrays.keySet().iterator(); }

  // Interns a String.
  // Delegates to the builtin String.intern() method.  Provided for
  // completeness, so we can intern() any type used in OneOf.java.jpp.
  public static /*@Interned*/ String intern(String a) {
    return (a == null) ? null : a.intern();
  }

  // Interns a long.
  // A no-op.  Provided for completeness, so we can intern() any type
  // used in OneOf.java.jpp.
  public static long intern(long l) {
    return l;
  }

  // Interns a long.
  // A no-op.  Provided for completeness, so we can intern() any type
  // used in OneOf.java.jpp.
  public static double intern(double l) {
    return l;
  }

  /**
   * Intern (canonicalize) an Integer.
   * Returns a canonical representation for the Integer.
   **/
  @SuppressWarnings("interned")
  public static /*@Interned*/ Integer intern(Integer a) {
    WeakReference</*@Interned*/ Integer> lookup = internedIntegers.get(a);
    if (lookup != null) {
      return lookup.get();
    } else {
      /*@Interned*/ Integer result = (/*@Interned*/ Integer) a; // cast is redundant (except in JSR 308)
      internedIntegers.put(result, new WeakReference</*@Interned*/ Integer>(result));
      return result;
    }
  }

  // Not sure whether this convenience method is really worth it.
  /** Returns an interned Integer with value i. */
  public static /*@Interned*/ Integer internedInteger(int i) {
    return intern(new Integer(i));
  }

  // Not sure whether this convenience method is really worth it.
  /** Returns an interned Integer with value parsed from the string. */
  public static /*@Interned*/ Integer internedInteger(String s) {
    return intern(Integer.decode(s));
  }


  /**
   * Intern (canonicalize) a Long.
   * Returns a canonical representation for the Long.
   **/
  @SuppressWarnings("interned")
  public static /*@Interned*/ Long intern(Long a) {
    WeakReference</*@Interned*/ Long> lookup =  internedLongs.get(a);
    if (lookup != null) {
      return lookup.get();
    } else {
      /*@Interned*/ Long result = (/*@Interned*/ Long) a; // cast is redundant (except in JSR 308)
      internedLongs.put(result, new WeakReference</*@Interned*/ Long>(result));
      return result;
    }
  }

  // Not sure whether this convenience method is really worth it.
  /** Returns an interned Long with value i. */
  public static /*@Interned*/ Long internedLong(long i) {
    return intern(new Long(i));
  }

  // Not sure whether this convenience method is really worth it.
  /** Returns an interned Long with value parsed from the string. */
  public static /*@Interned*/ Long internedLong(String s) {
    return intern(Long.decode(s));
  }


  // I might prefer to have the intern methods first check using a straight
  // eq hashing, which would be more efficient if the array is already
  // interned.  (How frequent do I expect that to be, and how much would
  // that really improve performance even in that case?)

  /**
   * Intern (canonicalize) an int[].
   * Returns a canonical representation for the int[] array.
   * Arrays are compared according to their elements.
   **/
  @SuppressWarnings("interned")
  public static /*@Interned*/ int[] intern(int[] a) {
    // Throwable stack = new Throwable("debug traceback");
    // stack.fillInStackTrace();
    // stack.printStackTrace();

    WeakReference</*@Interned*/ int[]> lookup = internedIntArrays.get(a);
    if (lookup != null) {
      return lookup.get();
    } else {
      /*@Interned*/ int[] result = (/*@Interned*/ int[]) a; // cast is redundant (except in JSR 308)
      internedIntArrays.put(result, new WeakReference</*@Interned*/ int[]>(result));
      return result;
    }
  }

  /**
   * Intern (canonicalize) a long[].
   * Returns a canonical representation for the long[] array.
   * Arrays are compared according to their elements.
   **/
  @SuppressWarnings("interned")
  public static /*@Interned*/ long[] intern(long[] a) {
    //System.out.printf ("intern %s %s long[] %s\n", a.getClass(),
    //                   a, Arrays.toString (a));
    WeakReference</*@Interned*/ long[]> lookup = internedLongArrays.get(a);
    if (lookup != null) {
      return lookup.get();
    } else {
      /*@Interned*/ long[] result = (/*@Interned*/ long[]) a; // cast is redundant (except in JSR 308)
      internedLongArrays.put(result, new WeakReference</*@Interned*/ long[]>(result));
      return result;
    }
  }

  /**
   * Intern (canonicalize) a Double.
   * Returns a canonical representation for the Double.
   **/
  @SuppressWarnings("interned")
  public static /*@Interned*/ Double intern(Double a) {
    // Double.NaN == Double.Nan  always evaluates to false.
    if (a.isNaN())
      return internedDoubleNaN;
    // Double.+0 == Double.-0,  but they compare true via equals()
    if (a.doubleValue() == 0)   // catches both positive and negative zero
      return internedDoubleZero;
    WeakReference</*@Interned*/ Double> lookup = internedDoubles.get(a);
    if (lookup != null) {
      return lookup.get();
    } else {
      /*@Interned*/ Double result = (/*@Interned*/ Double) a; // cast is redundant (except in JSR 308)
      internedDoubles.put(result, new WeakReference</*@Interned*/ Double>(result));
      return result;
    }
  }

  // Not sure whether this convenience method is really worth it.
  /** Returns an interned Double with value i. */
  public static /*@Interned*/ Double internedDouble(double d) {
    return intern(new Double(d));
  }

  // Not sure whether this convenience method is really worth it.
  /** Returns an interned Double with value parsed from the string. */
  public static /*@Interned*/ Double internedDouble(String s) {
    return internedDouble(Double.parseDouble(s));
  }


  // I might prefer to have the intern methods first check using a straight
  // eq hashing, which would be more efficient if the array is already
  // interned.  (How frequent do I expect that to be, and how much would
  // that really improve performance even in that case?)

  /**
   * Intern (canonicalize) a double[].
   * Returns a canonical representation for the double[] array.
   * Arrays are compared according to their elements.
   **/
  @SuppressWarnings("interned")
  public static /*@Interned*/ double[] intern(double[] a) {
    WeakReference</*@Interned*/ double[]> lookup = internedDoubleArrays.get(a);
    if (lookup != null) {
      return lookup.get();
    } else {
      /*@Interned*/ double[] result = (/*@Interned*/ double[]) a; // cast is redundant (except in JSR 308)
      internedDoubleArrays.put(result, new WeakReference</*@Interned*/ double[]>(result));
      return result;
    }
  }

  /**
   * Intern (canonicalize) an String[].
   * Returns a canonical representation for the String[] array.
   * Arrays are compared according to their elements.
   * The elements should themselves already be interned;
   * they are compared using their equals() methods.
   **/
  @SuppressWarnings("interned")
  public static /*@Interned*/ String[] intern(String[] a) {

    // Make sure each element is already interned
    if (Assert.enabled) {
      for (int k = 0; k < a.length; k++)
        Assert.assertTrue (a[k] == Intern.intern (a[k]));
    }

    WeakReference</*@Interned*/ String[]> lookup = internedStringArrays.get(a);
    if (lookup != null) {
      return lookup.get();
    } else {
      /*@Interned*/ String[] result = (/*@Interned*/ String[]) a; // cast is redundant (except in JSR 308)
      internedStringArrays.put(result, new WeakReference</*@Interned*/ String[]>(result));
      return result;
    }
  }

  /**
   * Intern (canonicalize) an Object[].
   * Returns a canonical representation for the Object[] array.
   * Arrays are compared according to their elements.
   * The elements should themselves already be interned;
   * they are compared using their equals() methods.
   **/
  @SuppressWarnings("interned")
  public static /*@Interned*/ Object[] intern(Object[] a) {
    WeakReference</*@Interned*/ Object[]> lookup = internedObjectArrays.get(a);
    if (lookup != null) {
      return lookup.get();
    } else {
      /*@Interned*/ Object[] result = (/*@Interned*/ Object[]) a; // cast is redundant (except in JSR 308)
      internedObjectArrays.put(result, new WeakReference</*@Interned*/ Object[]>(result));
      return result;
    }
  }


  /**
   * Convenince method to intern an Object when we don't know its
   * runtime type.  Its runtime type must be one of the types for
   * which we have an intern() method, else an exception is thrown.
   **/
  public static /*@Interned*/ Object intern(Object a) {
    if (a == null) {
      return null;
    } else if (a instanceof String) {
      return intern((String) a);
    } else if (a instanceof String[]) {
      return intern((String[]) a);
    } else if (a instanceof Integer) {
      return intern((Integer) a);
    } else if (a instanceof Long) {
      return intern((Long) a);
    } else if (a instanceof int[]) {
      return intern((int[]) a);
    } else if (a instanceof long[]) {
      return intern((long[]) a);
    } else if (a instanceof Double) {
      return intern((Double) a);
    } else if (a instanceof double[]) {
      return intern((double[]) a);
    } else if (a instanceof Object[]) {
      return intern((Object[]) a);
    } else {
      throw new IllegalArgumentException
        ("Arguments of type " + a.getClass() + " cannot be interned");
    }
  }

  /**
   * Return the subsequence of seq from start (inclusive) to end
   * (exclusive) that is interned.  What's different about this method
   * from manually findind the subsequence and interning the
   * subsequence is that if the subsequence is already interned, we
   * can avoid having to compute the sequence.  Since derived
   * variables in Daikon compute the subsequence many times, this
   * shortcut saves quite a bit of computation.  It saves even more
   * when there may be many derived variables that are non canonical,
   * since they are guaranteed to be ==.
   * <p>
   * Requires that seq is already interned
   * @return a subsequence of seq from start to end that is interned.
   **/
  public static int[] internSubsequence (/*@Interned*/ int[] seq, int start, int end) {
    Assert.assertTrue (Intern.isInterned(seq));
    SequenceAndIndices</*@Interned*/ int[]> sai = new SequenceAndIndices</*@Interned*/ int[]> (seq, start, end);
    WeakReference</*@Interned*/ int[]> lookup = internedIntSequenceAndIndices.get(sai);
    if (lookup != null) {
      return lookup.get();
    } else {
      int[] subseqUninterned = ArraysMDE.subarray(seq, start, end - start);
      /*@Interned*/ int[] subseq = Intern.intern (subseqUninterned);
      internedIntSequenceAndIndices.put (sai, new WeakReference</*@Interned*/ int[]>(subseq));
      return subseq;
    }
  }

  /**
   * @see #internSubsequence(int[], int, int)
   **/
  public static long[] internSubsequence (/*@Interned*/ long[] seq, int start, int end) {
    Assert.assertTrue (Intern.isInterned(seq));
    SequenceAndIndices</*@Interned*/ long[]> sai = new SequenceAndIndices</*@Interned*/ long[]> (seq, start, end);
    WeakReference</*@Interned*/ long[]> lookup = internedLongSequenceAndIndices.get(sai);
    if (lookup != null) {
      return lookup.get();
    } else {
      long[] subseq = ArraysMDE.subarray(seq, start, end - start);
      subseq = Intern.intern (subseq);
      internedLongSequenceAndIndices.put (sai, new WeakReference</*@Interned*/ long[]>(subseq));
      return subseq;
    }
  }

  /**
   * @see #internSubsequence(int[], int, int)
   **/
  public static double[] internSubsequence (/*@Interned*/ double[] seq, int start, int end) {
    Assert.assertTrue (Intern.isInterned(seq));
    SequenceAndIndices</*@Interned*/ double[]> sai = new SequenceAndIndices</*@Interned*/ double[]> (seq, start, end);
    WeakReference</*@Interned*/ double[]> lookup = internedDoubleSequenceAndIndices.get(sai);
    if (lookup != null) {
      return lookup.get();
    } else {
      double[] subseq = ArraysMDE.subarray(seq, start, end - start);
      subseq = Intern.intern (subseq);
      internedDoubleSequenceAndIndices.put (sai, new WeakReference</*@Interned*/ double[]>(subseq));
      return subseq;
    }
  }

  /**
   * @see #internSubsequence(int[], int, int)
   **/
  public static Object[] internSubsequence (/*@Interned*/ Object[] seq, int start, int end) {
    Assert.assertTrue (Intern.isInterned(seq));
    SequenceAndIndices</*@Interned*/ Object[]> sai = new SequenceAndIndices</*@Interned*/ Object[]> (seq, start, end);
    WeakReference</*@Interned*/ Object[]> lookup = internedObjectSequenceAndIndices.get(sai);
    if (lookup != null) {
      return lookup.get();
    } else {
      Object[] subseq = ArraysMDE.subarray(seq, start, end - start);
      subseq = Intern.intern (subseq);
      internedObjectSequenceAndIndices.put (sai, new WeakReference</*@Interned*/ Object[]>(subseq));
      return subseq;
    }
  }

  /**
   * @see #internSubsequence(int[], int, int)
   **/
  public static String[] internSubsequence (/*@Interned*/ String[] seq, int start, int end) {
    Assert.assertTrue (Intern.isInterned(seq));
    SequenceAndIndices</*@Interned*/ String[]> sai = new SequenceAndIndices</*@Interned*/ String[]> (seq, start, end);
    WeakReference</*@Interned*/ String[]> lookup = internedStringSequenceAndIndices.get(sai);
    if (lookup != null) {
      return lookup.get();
    } else {
      String[] subseq = ArraysMDE.subarray(seq, start, end - start);
      subseq = Intern.intern (subseq);
      internedStringSequenceAndIndices.put (sai, new WeakReference</*@Interned*/ String[]>(subseq));
      return subseq;
    }
  }

  /**
   * Data structure for storing triples of a sequence and start and
   * end indices, to represent a subsequence.  Requires that the
   * sequence be interned.  Used for interning the repeated finding
   * of subsequences on the same sequence.
   **/
  private static final class SequenceAndIndices<T extends /*@Interned*/ Object> {
    public T seq;
    public int start;
    public int end;

    /**
     * @param seq An interned array
     **/
    public SequenceAndIndices (T seq, int start, int end) {
      this.seq = seq;
      this.start = start;
      this.end = end;
      assert isInterned(seq);
    }

    @SuppressWarnings("unchecked")
    public boolean equals (Object other) {
      if (other instanceof SequenceAndIndices) {
        return equals((SequenceAndIndices<T>) other); // unchecked
      } else {
        return false;
      }
    }

    public boolean equals (SequenceAndIndices<T> other) {
      return (this.seq == other.seq) &&
        this.start == other.start &&
        this.end == other.end;
    }

    public int hashCode() {
      return seq.hashCode() + start * 30 - end * 2;
    }

    // For debugging
    public String toString() {
      return "SAI(" + start + "," + end + ") from: " + ArraysMDE.toString(seq);
    }

  }

  /**
   * Hasher object which hashes and compares String[] objects according
   * to their contents.
   * @see Hasher
   **/
  private static final class SequenceAndIndicesHasher<T> implements Hasher {
    public boolean equals(Object a1, Object a2) {
      SequenceAndIndices<T> sai1 = (SequenceAndIndices<T>) a1; // unchecked cast
      SequenceAndIndices<T> sai2 = (SequenceAndIndices<T>) a2; // unchecked cast
      // The SAI objects are *not* interned, but the arrays inside them are.
      return sai1.equals(sai2);
    }

    public int hashCode(Object o) {
      return o.hashCode();
    }
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Interning arrays:  old implementation #1
  ///

  /// Interning arrays:  old implmentation.
  /// The problem with this is that it doesn't release keys.
  // // I can also use java.util.Arrays.equals() to compare two arrays of base
  // // or Object type; but that doesn't do ordering.  (It does properly deal
  // // with the possibility that the argument is null, which this doesn't
  // // right now.  I may want to err in this implementation if the arguments
  // // are null or the lengths are not equal -- if I never mix arrays of
  // // different lengths.)

  // // Note: this comparator imposes orderings that are inconsistent with equals.
  // // That is, it may return 0 if the arrays are not equal (but do contain
  // // identical numbers).
  // static final class IntArrayComparator implements Comparator {
  //   public int compare(Object o1, Object o2) {
  //     if (o1 == o2)
  //       return 0;
  //     int[] a1 = (int[])o1;
  //     int[] a2 = (int[])o2;
  //     int tmp;
  //     tmp = a1.length - a2.length;
  //     if (tmp != 0)
  //       return tmp;
  //     for (int i=0; i<a1.length; i++) {
  //       tmp = a1[i] - a2[i];
  //       if (tmp != 0)
  //         return tmp;
  //     }
  //     return 0;
  //   }
  // }

  // // Note: this comparator imposes orderings that are inconsistent with equals.
  // // That is, it may return 0 if the arrays are not equal (but do contain
  // // identical objects).
  // static final class ObjectArrayComparator implements Comparator {
  //   public int compare(Object o1, Object o2) {
  //     if (o1 == o2)
  //       return 0;
  //     Object[] a1 = (Object[])o1;
  //     Object[] a2 = (Object[])o2;
  //     int tmp;
  //     tmp = a1.length - a2.length;
  //     if (tmp != 0)
  //       return tmp;
  //     for (int i=0; i<a1.length; i++) {
  //       tmp = a1[i].hashCode() - a2[i].hashCode();
  //       if (tmp != 0)
  //         return tmp;
  //       // I'm counting on the fact that hashCode returns a different
  //       // number for each Object in the system.  This checks that assumption.
  //       Assert.assertTrue(a1[i].equals(a2[i]));
  //     }
  //     return 0;
  //   }
  // }

  // private static TreeSet internedIntArrays;
  // private static TreeSet internedObjectArrays;

  // static {
  //   internedIntArrays = new TreeSet(new IntArrayComparator());
  //   internedObjectArrays = new TreeSet(new ObjectArrayComparator());
  // }

  // public static int[] internIntArray(int[] a) {
  //   boolean added = internedIntArrays.add(a);
  //   if (added)
  //     return a;
  //   else
  //     return (int[])internedIntArrays.tailSet(a).first();
  // }

  // // All the elements should already themselves be interned
  // public static Object[] internObjectArray(Object[] a) {
  //   boolean added = internedObjectArrays.add(a);
  //   if (added)
  //     return a;
  //   else
  //     return (Object[])internedObjectArrays.tailSet(a).first();
  // }



  ///////////////////////////////////////////////////////////////////////////
  /// Interning arrays:  old implementation #2
  ///

  /// This doesn't work because there are no references to the Wrappers,
  /// so all of the WeakHashMap elements are immediately removed.

  // // Create an ArrayWrapper which redefines equal (and hash) to act the
  // // way I want them to.

  // static final class IntArrayWrapper {
  //   private int[] a;
  //   IntArrayWrapper(int[] a) {
  //     this.a = a;
  //   }
  //   boolean equals(IntArrayWrapper other) {
  //     return java.util.Arrays.equals(a, other.a);
  //   }
  //   static final int FACTOR = 23;
  //   public int hashCode() {
  //     int result = 0;
  //     for (int i=0; i<a.length; i++) {
  //       result = result * FACTOR + a[i];
  //     }
  //     return result;
  //   }
  // }

  // static final class ObjectArrayWrapper {
  //   private Object[] a;
  //   ObjectArrayWrapper(Object[] a) {
  //     this.a = a;
  //   }
  //   boolean equals(ObjectArrayWrapper other) {
  //     return java.util.Arrays.equals(a, other.a);
  //   }
  //   static final int FACTOR = 23;
  //   // Alternately, just xor all the element hash codes.
  //   public int hashCode() {
  //     int result = 0;
  //     for (int i=0; i<a.length; i++) {
  //       result = result * FACTOR + a[i].hashCode();
  //     }
  //     return result;
  //   }
  // }

  // // Map from an ArrayWrapper to the array (I don't need to map to a
  // // WeakReference because the array isn't the key of the WeakHashMap).

  // // non-private for debugging only
  // static WeakHashMap internedIntArrays;
  // static WeakHashMap internedObjectArrays;
  // // private static WeakHashMap internedIntArrays;
  // // private static WeakHashMap internedObjectArrays;

  // static {
  //   internedIntArrays = new WeakHashMap();
  //   internedObjectArrays = new WeakHashMap();
  // }

  // public static int[] internIntArray(int[] a) {
  //   IntArrayWrapper w = new IntArrayWrapper(a);
  //   Object result = internedIntArrays.get(w);
  //   if (result != null)
  //     return (int[])result;
  //   else {
  //     internedIntArrays.put(w, a);
  //     return a;
  //   }
  // }

  // // All the elements should already themselves be interned
  // public static Object[] internObjectArray(Object[] a) {
  //   ObjectArrayWrapper w = new ObjectArrayWrapper(a);
  //   Object result = internedObjectArrays.get(w);
  //   if (result != null)
  //     return (Object[])result;
  //   else {
  //     internedObjectArrays.put(w, a);
  //     return a;
  //   }
  // }

}
