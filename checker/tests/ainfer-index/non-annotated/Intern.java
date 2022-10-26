package org.plumelib.util;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.common.value.qual.PolyValue;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/**
 * Utilities for interning objects. Interning is also known as canonicalization or hash-consing: it
 * returns a single representative object that {@link Object#equals} the object, and the client
 * discards the argument and uses the result instead. Since only one object exists for every set of
 * equal objects, space usage is reduced. Time may also be reduced, since it is possible to use
 * {@code ==} instead of {@code .equals()} for comparisons.
 *
 * <p>Java builds in interning for Strings, but not for other objects. The methods in this class
 * extend interning to all Java objects.
 */
public final class Intern {

  /** This class is a collection of methods; it does not represent anything. */
  private Intern() {
    throw new Error("do not instantiate");
  }

  /** Whether assertions are enabled. */
  private static boolean assertsEnabled = false;

  static {
    assert assertsEnabled = true; // Intentional side-effect!!!
    // Now assertsEnabled is set to the correct value
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Strings
  ///

  /**
   * Replace each element of the array by its interned version. Side-effects the array, but also
   * returns it.
   *
   * @param a the array whose elements to intern in place
   * @return an interned version of a
   * @see String#intern
   */
  @SuppressWarnings("interning") // side-effects the array in place (dangerous, but convenient)
  public static @Interned String @PolyValue @SameLen("#1") [] internStrings(
      String @PolyValue [] a) {
    for (int i = 0; i < a.length; i++) {
      if (a[i] != null) {
        a[i] = a[i].intern();
      }
    }
    return a;
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Testing interning
  ///

  /**
   * Returns true if the argument is interned (is canonical among all objects equal to itself).
   *
   * @param value the value to test for interning
   * @return true iff value is interned
   */
  @SuppressWarnings({"interning"}) // interning implementation
  @Pure
  public static boolean isInterned(@Nullable Object value) {
    if (value == null) {
      // nothing to do
      return true;
    } else if (value instanceof String) {
      return value == ((String) value).intern();
    } else if (value instanceof String[]) {
      return value == intern((String[]) value);
    } else if (value instanceof Integer) {
      return value == intern((Integer) value);
    } else if (value instanceof Long) {
      return value == intern((Long) value);
    } else if (value instanceof int[]) {
      return value == intern((int[]) value);
    } else if (value instanceof long[]) {
      return value == intern((long[]) value);
    } else if (value instanceof Double) {
      return value == intern((Double) value);
    } else if (value instanceof double[]) {
      return value == intern((double[]) value);
    } else if (value instanceof Object[]) {
      return value == intern((Object[]) value);
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
   * Hasher object which hashes and compares Integers. This is the obvious implementation that uses
   * intValue() for the hashCode.
   *
   * @see Hasher
   */
  private static final class IntegerHasher implements Hasher {
    /** Create a new IntegerHasher. */
    public IntegerHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return a1.equals(a2);
    }

    @Override
    public int hashCode(Object o) {
      Integer i = (Integer) o;
      return i.intValue();
    }
  }

  /**
   * Hasher object which hashes and compares Longs. This is the obvious implementation that uses
   * intValue() for the hashCode.
   *
   * @see Hasher
   */
  private static final class LongHasher implements Hasher {
    /** Create a new LongHasher. */
    public LongHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return a1.equals(a2);
    }

    @Override
    public int hashCode(Object o) {
      Long i = (Long) o;
      return i.intValue();
    }
  }

  /**
   * Hasher object which hashes and compares int[] objects according to their contents.
   *
   * @see Hasher
   * @see Arrays#equals(int[], int[])
   */
  private static final class IntArrayHasher implements Hasher {
    /** Create a new IntArrayHasher. */
    public IntArrayHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return Arrays.equals((int[]) a1, (int[]) a2);
    }

    @Override
    public int hashCode(Object o) {
      return Arrays.hashCode((int[]) o);
    }
  }

  /**
   * Hasher object which hashes and compares long[] objects according to their contents.
   *
   * @see Hasher
   * @see Arrays#equals (long[], long[])
   */
  private static final class LongArrayHasher implements Hasher {
    /** Create a new LongArrayHasher. */
    public LongArrayHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return Arrays.equals((long[]) a1, (long[]) a2);
    }

    @Override
    public int hashCode(Object o) {
      return Arrays.hashCode((long[]) o);
    }
  }

  /** Multiplicative constant for use in hashing function. */
  private static final int FACTOR = 23;
  /** Another multiplicative constant for use in hashing function. */
  private static final double DOUBLE_FACTOR = 263;

  /**
   * Hasher object which hashes and compares Doubles.
   *
   * @see Hasher
   */
  private static final class DoubleHasher implements Hasher {
    /** Create a new DoubleHasher. */
    public DoubleHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return a1.equals(a2);
    }

    @Override
    public int hashCode(Object o) {
      Double d = (Double) o;
      return d.hashCode();
    }
  }

  /**
   * Hasher object which hashes and compares double[] objects according to their contents.
   *
   * @see Hasher
   * @see Arrays#equals(Object[],Object[])
   */
  private static final class DoubleArrayHasher implements Hasher {
    /** Create a new DoubleArrayHasher. */
    public DoubleArrayHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      // "Arrays.equals" considers +0.0 != -0.0.
      // Also, it gives inconsistent results (on different JVMs/classpaths?).
      // return Arrays.equals((double[])a1, (double[])a2);
      double[] da1 = (double[]) a1;
      double[] da2 = (double[]) a2;
      if (da1.length != da2.length) {
        return false;
      }
      for (int i = 0; i < da1.length; i++) {
        if (!((da1[i] == da2[i]) || (Double.isNaN(da1[i]) && Double.isNaN(da2[i])))) {
          return false;
        }
      }
      return true;
    }

    @Override
    public int hashCode(Object o) {
      double[] a = (double[]) o;
      // Not Arrays.hashCode(a), for consistency with equals method
      // immediately above.
      double running = 0;
      for (int i = 0; i < a.length; i++) {
        double elt = (Double.isNaN(a[i]) ? 0.0 : a[i]);
        running = running * FACTOR + elt * DOUBLE_FACTOR;
      }
      // Could add "... % Integer.MAX_VALUE" here; is that good to do?
      long result = Math.round(running);
      return (int) (result % Integer.MAX_VALUE);
    }
  }

  /**
   * Hasher object which hashes and compares String[] objects according to their contents.
   *
   * @see Hasher
   * @see Arrays#equals
   */
  private static final class StringArrayHasher implements Hasher {
    /** Create a new StringArrayHasher. */
    public StringArrayHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return Arrays.equals((String[]) a1, (String[]) a2);
    }

    @Override
    public int hashCode(Object o) {
      return Arrays.hashCode((String[]) o);
    }
  }

  /**
   * Hasher object which hashes and compares Object[] objects according to their contents.
   *
   * @see Hasher
   * @see Arrays#equals(Object[], Object[])
   */
  private static final class ObjectArrayHasher implements Hasher {
    /** Create a new ObjectArrayHasher. */
    public ObjectArrayHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      return Arrays.equals((@Nullable Object[]) a1, (@Nullable Object[]) a2);
    }

    @Override
    public int hashCode(Object o) {
      return Arrays.hashCode((Object[]) o);
    }
  }

  // Each of these maps has:
  //   key = an interned object
  //   value = a WeakReference for the object itself.
  // They can be looked up using a non-interned value; equality tests know
  // nothing of the interning types.

  /** All the interned Integers. */
  private static WeakHasherMap<@Interned Integer, WeakReference<@Interned Integer>>
      internedIntegers;
  /** All the interned Longs. */
  private static WeakHasherMap<@Interned Long, WeakReference<@Interned Long>> internedLongs;
  /** All the interned Int arrays. */
  private static WeakHasherMap<int @Interned [], WeakReference<int @Interned []>> internedIntArrays;
  /** All the interned Long arrays. */
  private static WeakHasherMap<long @Interned [], WeakReference<long @Interned []>>
      internedLongArrays;
  /** All the interned Doubles. */
  private static WeakHasherMap<@Interned Double, WeakReference<@Interned Double>> internedDoubles;
  /** The interned NaN. */
  private static @Interned Double internedDoubleNaN;
  /** The interned Double zero. */
  private static @Interned Double internedDoubleZero;
  /** All the interned Double arrays. */
  private static WeakHasherMap<double @Interned [], WeakReference<double @Interned []>>
      internedDoubleArrays;
  /** All the interned String arrays. */
  private static WeakHasherMap<
      @Nullable @Interned String @Interned [],
      WeakReference<@Nullable @Interned String @Interned []>>
      internedStringArrays;
  /** All the interned Object arrays. */
  private static WeakHasherMap<
      @Nullable @Interned Object @Interned [],
      WeakReference<@Nullable @Interned Object @Interned []>>
      internedObjectArrays;
  /** All the interned Int subsequences. */
  private static WeakHasherMap<Subsequence<int @Interned []>, WeakReference<int @Interned []>>
      internedIntSubsequence;
  /** All the interned Long subsequences. */
  private static WeakHasherMap<Subsequence<long @Interned []>, WeakReference<long @Interned []>>
      internedLongSubsequence;
  /** All the interned Double subsequences. */
  private static WeakHasherMap<Subsequence<double @Interned []>, WeakReference<double @Interned []>>
      internedDoubleSubsequence;
  /** All the interned Object subsequences. */
  private static WeakHasherMap<
      Subsequence<@Nullable @Interned Object @Interned []>,
      WeakReference<@Nullable @Interned Object @Interned []>>
      internedObjectSubsequence;
  /** All the interned String subsequences. */
  private static WeakHasherMap<
      Subsequence<@Nullable @Interned String @Interned []>,
      WeakReference<@Nullable @Interned String @Interned []>>
      internedStringSubsequence;

  static {
    internedIntegers = new WeakHasherMap<>(new IntegerHasher());
    internedLongs = new WeakHasherMap<>(new LongHasher());
    internedIntArrays = new WeakHasherMap<>(new IntArrayHasher());
    internedLongArrays = new WeakHasherMap<>(new LongArrayHasher());
    internedDoubles = new WeakHasherMap<>(new DoubleHasher());
    internedDoubleNaN = Double.NaN;
    internedDoubleZero = 0.0;
    internedDoubleArrays = new WeakHasherMap<>(new DoubleArrayHasher());
    internedStringArrays = new WeakHasherMap<>(new StringArrayHasher());
    internedObjectArrays =
        new WeakHasherMap<
            @Nullable @Interned Object @Interned [],
            WeakReference<@Nullable @Interned Object @Interned []>>(new ObjectArrayHasher());
    internedIntSubsequence = new WeakHasherMap<>(new SubsequenceHasher<int @Interned []>());
    internedLongSubsequence = new WeakHasherMap<>(new SubsequenceHasher<long @Interned []>());
    internedDoubleSubsequence = new WeakHasherMap<>(new SubsequenceHasher<double @Interned []>());
    internedObjectSubsequence =
        new WeakHasherMap<>(new SubsequenceHasher<@Nullable @Interned Object @Interned []>());
    internedStringSubsequence =
        new WeakHasherMap<>(new SubsequenceHasher<@Nullable @Interned String @Interned []>());
  }

  /// For testing only

  /**
   * Returns the number of interned integers. For testing only.
   *
   * @return the number of interned integers
   */
  static int numIntegers() {
    return internedIntegers.size();
  }

  /**
   * Returns the number of interned longs. For testing only.
   *
   * @return the number of interned longs
   */
  static int numLongs() {
    return internedLongs.size();
  }

  /**
   * Returns the number of interned int arrays. For testing only.
   *
   * @return the number of interned int arrays
   */
  static int numIntArrays() {
    return internedIntArrays.size();
  }

  /**
   * Returns the number of interned long arrays. For testing only.
   *
   * @return the number of interned long arrays
   */
  static int numLongArrays() {
    return internedLongArrays.size();
  }

  /**
   * Returns the number of interned doubles. For testing only.
   *
   * @return the number of interned doubles
   */
  static int numDoubles() {
    return internedDoubles.size();
  }

  /**
   * Returns the number of interned double arrays. For testing only.
   *
   * @return the number of interned double arrays
   */
  static int numDoubleArrays() {
    return internedDoubleArrays.size();
  }

  /**
   * Returns the number of interned string arrays. For testing only.
   *
   * @return the number of interned string arrays
   */
  static int numStringArrays() {
    return internedStringArrays.size();
  }

  /**
   * Returns the number of interned object arrays. For testing only.
   *
   * @return the number of interned object arrays
   */
  static int numObjectArrays() {
    return internedObjectArrays.size();
  }

  /**
   * Returns all the interned integers. For testing only.
   *
   * @return all the interned integers
   */
  static Iterator<@Interned Integer> integers() {
    return internedIntegers.keySet().iterator();
  }

  /**
   * Returns all the interned longs. For testing only.
   *
   * @return all the interned longs
   */
  static Iterator<@Interned Long> longs() {
    return internedLongs.keySet().iterator();
  }

  /**
   * Returns all the interned int arrays. For testing only.
   *
   * @return all the interned int arrays
   */
  static Iterator<int @Interned []> intArrays() {
    return internedIntArrays.keySet().iterator();
  }

  /**
   * Returns all the interned long arrays. For testing only.
   *
   * @return all the interned long arrays
   */
  static Iterator<long @Interned []> longArrays() {
    return internedLongArrays.keySet().iterator();
  }

  /**
   * Returns all the interned doubles. For testing only.
   *
   * @return all the interned doubles
   */
  static Iterator<@Interned Double> doubles() {
    return internedDoubles.keySet().iterator();
  }

  /**
   * Returns all the interned double arrays. For testing only.
   *
   * @return all the interned double arrays
   */
  static Iterator<double @Interned []> doubleArrays() {
    return internedDoubleArrays.keySet().iterator();
  }

  /**
   * Returns all the interned string arrays. For testing only.
   *
   * @return all the interned string arrays
   */
  static Iterator<@Nullable @Interned String @Interned []> stringArrays() {
    return internedStringArrays.keySet().iterator();
  }

  /**
   * Returns all the interned object arrays. For testing only.
   *
   * @return all the interned object arrays
   */
  static Iterator<@Nullable @Interned Object @Interned []> objectArrays() {
    return internedObjectArrays.keySet().iterator();
  }

  /// End of testing methods

  /**
   * Interns a String. Delegates to the builtin String.intern() method, but handles {@code null}.
   *
   * @param a the string to intern; may be null
   * @return an interned version of the argument, or null if the argument was null
   */
  @Pure
  public static @Interned @PolyNull @PolyValue @SameLen("#1") String intern(
      @PolyNull @PolyValue String a) {
    return (a == null) ? null : a.intern();
  }

  /**
   * Interns a long. A no-op. Provided for completeness.
   *
   * @param l the long to intern
   * @return an interned version of the argument
   */
  @Pure
  public static long intern(long l) {
    return l;
  }

  /**
   * Interns a double A no-op. Provided for completeness.
   *
   * @param d the double to intern
   * @return an interned version of the argument
   */
  @Pure
  public static double intern(double d) {
    return d;
  }

  /**
   * Intern (canonicalize) an Integer. Return a canonical representation for the Integer.
   *
   * @param a an Integer to canonicalize
   * @return a canonical representation for the Integer
   */
  // TODO: JLS 5.1.7 requires that the boxing conversion interns integer
  // values between -128 and 127 (and Intern.valueOf is intended to promise
  // the same).  This does not currently take advantage of that.
  @SuppressWarnings({"interning", "allcheckers:purity", "lock"}) // interning implementation
  @Pure
  public static @Interned Integer intern(Integer a) {
    WeakReference<@Interned Integer> lookup = internedIntegers.get(a);
    Integer result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @Interned Integer result = (@Interned Integer) a;
      internedIntegers.put(result, new WeakReference<>(result));
      return result;
    }
  }

  // Not sure whether this convenience method is really worth it.
  /**
   * Returns an interned Integer with value i.
   *
   * @param i the value to intern
   * @return an interned Integer with value i
   */
  public static @Interned Integer internedInteger(int i) {
    return intern(Integer.valueOf(i));
  }

  // Not sure whether this convenience method is really worth it.
  /**
   * Returns an interned Integer with value parsed from the string.
   *
   * @param s the string to parse
   * @return an interned Integer parsed from s
   */
  public static @Interned Integer internedInteger(String s) {
    return intern(Integer.decode(s));
  }

  /**
   * Intern (canonicalize) a Long. Return a canonical representation for the Long.
   *
   * @param a the value to intern
   * @return a canonical representation for the Long
   */
  // TODO: JLS 5.1.7 requires that the boxing conversion interns integer
  // values between -128 and 127 (and Long.valueOf is intended to promise
  // the same).  This could take advantage of that.
  @SuppressWarnings({"interning", "allcheckers:purity", "lock"})
  @Pure
  public static @Interned Long intern(Long a) {
    WeakReference<@Interned Long> lookup = internedLongs.get(a);
    Long result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @Interned Long result = (@Interned Long) a;
      internedLongs.put(result, new WeakReference<>(result));
      return result;
    }
  }

  // Not sure whether this convenience method is really worth it.
  /**
   * Returns an interned Long with value i.
   *
   * @param i the value to intern
   * @return an interned Integer with value i
   */
  public static @Interned Long internedLong(long i) {
    return intern(Long.valueOf(i));
  }

  // Not sure whether this convenience method is really worth it.
  /**
   * Returns an interned Long with value parsed from the string.
   *
   * @param s the string to parse
   * @return an interned Long parsed from s
   */
  public static @Interned Long internedLong(String s) {
    return intern(Long.decode(s));
  }

  // I might prefer to have the intern methods first check using a straight
  // eq hashing, which would be more efficient if the array is already
  // interned.  (How frequent do I expect that to be, and how much would
  // that really improve performance even in that case?)

  /**
   * Intern (canonicalize) an int[]. Return a canonical representation for the int[] array. Arrays
   * are compared according to their elements.
   *
   * @param a the array to canonicalize
   * @return a canonical representation for the int[] array
   */
  @SuppressWarnings({"interning", "allcheckers:purity", "lock"})
  @Pure
  public static int @Interned @PolyValue @SameLen("#1") [] intern(int @PolyValue [] a) {
    // Throwable stack = new Throwable("debug traceback");
    // stack.fillInStackTrace();
    // stack.printStackTrace();

    WeakReference<int @Interned []> lookup = internedIntArrays.get(a);
    @SuppressWarnings({
        "samelen:assignment", // for this map, get() can be annotated as
        // @SameLen("#1")
        "value" // for this map, get() can be annotated as @PolyAll (except not interning); also see
        // https://github.com/kelloggm/checker-framework/issues/177
    })
    int @PolyValue @SameLen("a") [] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @Interned int[] result = (int @Interned @PolyValue []) a;
      internedIntArrays.put(result, new WeakReference<>(result));
      return result;
    }
  }

  /**
   * Intern (canonicalize) a long[]. Return a canonical representation for the long[] array. Arrays
   * are compared according to their elements.
   *
   * @param a the array to canonicalize
   * @return a canonical representation for the long[] array
   */
  @SuppressWarnings({"interning", "allcheckers:purity", "lock"})
  @Pure
  public static long @Interned @PolyValue @SameLen("#1") [] intern(long @PolyValue [] a) {
    // System.out.printf("intern %s %s long[] %s%n", a.getClass(),
    //                   a, Arrays.toString (a));
    WeakReference<long @Interned []> lookup = internedLongArrays.get(a);
    @SuppressWarnings({
        "samelen:assignment", // for this map, get() can be annotated as
        // @SameLen("#1")
        "value" // for this map, get() can be annotated as @PolyAll (except not interning); also see
        // https://github.com/kelloggm/checker-framework/issues/177
    })
    long @PolyValue @SameLen("a") [] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @Interned long[] result = (long @Interned @PolyValue []) a;
      internedLongArrays.put(result, new WeakReference<>(result));
      return result;
    }
  }

  /**
   * Intern (canonicalize) a Double. Return a canonical representation for the Double.
   *
   * @param a the Double to canonicalize
   * @return a canonical representation for the Double
   */
  // TODO: JLS 5.1.7 requires that the boxing conversion interns integer
  // values between -128 and 127 (and Double.valueOf is intended to promise
  // the same).  This could take advantage of that.
  @SuppressWarnings({"interning", "allcheckers:purity", "lock"})
  @Pure
  public static @Interned Double intern(Double a) {
    // Double.NaN == Double.Nan  always evaluates to false.
    if (a.isNaN()) {
      return internedDoubleNaN;
    }
    // Double.+0 == Double.-0,  but they compare true via equals()
    if (a.doubleValue() == 0) { // catches both positive and negative zero
      return internedDoubleZero;
    }
    WeakReference<@Interned Double> lookup = internedDoubles.get(a);
    Double result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @Interned Double result = (@Interned Double) a;
      internedDoubles.put(result, new WeakReference<>(result));
      return result;
    }
  }

  // Not sure whether this convenience method is really worth it.
  /**
   * Returns an interned Double with value i.
   *
   * @param d the value to intern
   * @return an interned Double with value d
   */
  public static @Interned Double internedDouble(double d) {
    return intern(Double.valueOf(d));
  }

  // Not sure whether this convenience method is really worth it.
  /**
   * Returns an interned Double with value parsed from the string.
   *
   * @param s the string to parse
   * @return an interned Double parsed from s
   */
  public static @Interned Double internedDouble(String s) {
    return internedDouble(Double.parseDouble(s));
  }

  // I might prefer to have the intern methods first check using a straight
  // eq hashing, which would be more efficient if the array is already
  // interned.  (How frequent do I expect that to be, and how much would
  // that really improve performance even in that case?)

  /**
   * Intern (canonicalize) a double[]. Return a canonical representation for the double[] array.
   * Arrays are compared according to their elements.
   *
   * @param a the array to canonicalize
   * @return a canonical representation for the double[] array
   */
  @SuppressWarnings({"interning", "allcheckers:purity", "lock"})
  @Pure
  public static double @Interned @PolyValue @SameLen("#1") [] intern(double @PolyValue [] a) {
    WeakReference<double @Interned []> lookup = internedDoubleArrays.get(a);
    @SuppressWarnings({
        "samelen:assignment", // for this map, get() can be annotated as
        // @SameLen("#1")
        "value" // for this map, get() can be annotated as @PolyAll (except not interning); also see
        // https://github.com/kelloggm/checker-framework/issues/177
    })
    double @PolyValue @SameLen("a") [] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @Interned double[] result = (double @Interned @PolyValue []) a;
      internedDoubleArrays.put(result, new WeakReference<>(result));
      return result;
    }
  }

  /**
   * Intern (canonicalize) a String[]. Return a canonical representation for the String[] array.
   * Arrays are compared according to their elements' equals() methods.
   *
   * @param a the array to canonicalize. Its elements should already be interned.
   * @return a canonical representation for the String[] array
   */
  @SuppressWarnings({
      // Java warnings
      "cast",
      // Checker Framework warnings
      "interning", // interns its argument
      "allcheckers:purity",
      "lock",
      // Error Prone Warnings
      "ReferenceEquality"
  }) // cast is redundant (except in JSR 308)
  @Pure
  public static @PolyNull @Interned String @Interned @PolyValue @SameLen("#1") [] intern(
      @PolyNull @Interned String @PolyValue [] a) {

    // Make sure each element is already interned
    if (assertsEnabled) {
      for (int k = 0; k < a.length; k++) {
        if (a[k] != Intern.intern(a[k])) {
          throw new IllegalArgumentException();
        }
      }
    }

    WeakReference<@Nullable @Interned String @Interned []> lookup = internedStringArrays.get(a);
    @Nullable @Interned String @Interned [] result = (lookup != null) ? lookup.get() : null;
    if (result == null) {
      result = (@Nullable @Interned String @Interned []) a;
      internedStringArrays.put(result, new WeakReference<>(result));
    }
    @SuppressWarnings({
        "nullness", // for this map, get() can be annotated as @PolyAll (except not interning); also
        // see https://github.com/kelloggm/checker-framework/issues/177
        "samelen:assignment", // for this map, get() can be annotated as
        // @SameLen("#1")
        "value" // for this map, get() can be annotated as @PolyAll (except not interning); also see
        // https://github.com/kelloggm/checker-framework/issues/177
    })
    @PolyNull @Interned String @Interned @PolyValue @SameLen("a") [] polyresult = result;
    return polyresult;
  }

  /**
   * Intern (canonicalize) an Object[]. Return a canonical representation for the Object[] array.
   * Arrays are compared according to their elements. The elements should themselves already be
   * interned; they are compared using their equals() methods.
   *
   * @param a the array to canonicalize
   * @return a canonical representation for the Object[] array
   */
  @SuppressWarnings({
      "interning", // interns its argument
      "allcheckers:purity",
      "lock",
      "cast"
  }) // cast is redundant (except in JSR 308)
  @Pure
  public static @PolyNull @Interned Object @Interned @PolyValue @SameLen("#1") [] intern(
      @PolyNull @Interned @PolyValue Object[] a) {
    WeakReference<@Nullable @Interned Object @Interned []> lookup = internedObjectArrays.get(a);
    @Nullable @Interned Object @Interned [] result = (lookup != null) ? lookup.get() : null;
    if (result == null) {
      result = (@Nullable @Interned Object @Interned []) a;
      internedObjectArrays.put(result, new WeakReference<>(result));
    }
    @SuppressWarnings({
        "nullness", // for this map, get() can be annotated as @PolyAll (except not interning); also
        // see https://github.com/kelloggm/checker-framework/issues/177
        "samelen:assignment", // for this map, get() can be annotated as
        // @SameLen("#1")
        "value" // for this map, get() can be annotated as @PolyAll (except not interning); also see
        // https://github.com/kelloggm/checker-framework/issues/177
    }) // PolyNull/PolyValue:  value = parameter a, so same type & nullness as for parameter a
    @PolyNull @Interned Object @Interned @PolyValue @SameLen("a") [] polyresult = result;
    return polyresult;
  }

  /**
   * Convenience method to intern an Object when we don't know its run-time type. Its run-time type
   * must be one of the types for which we have an intern() method, else an exception is thrown. If
   * the argument is an array, its elements should themselves be interned.
   *
   * @param a an Object to canonicalize
   * @return a canonical version of a
   */
  @Pure
  public static @Interned @PolyNull Object intern(@PolyNull Object a) {
    if (a == null) {
      return null;
    } else if (a instanceof String) {
      return intern((String) a);
    } else if (a instanceof String[]) {
      @Interned String[] asArray = (@Interned String[]) a;
      return intern(asArray);
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
      @Interned Object[] asArray = (@Interned Object[]) a;
      return intern(asArray);
    } else {
      throw new IllegalArgumentException(
          "Arguments of type " + a.getClass() + " cannot be interned");
    }
  }

  /**
   * Returns an interned subsequence of seq from start (inclusive) to end (exclusive). The argument
   * seq should already be interned.
   *
   * <p>The result is the same as computing the subsequence and then interning it, but this method
   * is more efficient: if the subsequence is already interned, it avoids computing the subsequence.
   *
   * <p>For example, since derived variables in Daikon compute the subsequence many times, this
   * shortcut saves quite a bit of computation. It saves even more when there may be many derived
   * variables that are non-canonical, since they are guaranteed to be ==.
   *
   * @param seq the interned sequence whose subsequence should be computed and interned
   * @param start the index of the start of the subsequence to compute and intern
   * @param end the index of the end of the subsequence to compute and intern
   * @return a subsequence of seq from start to end that is interned
   */
  public static int @Interned [] internSubsequence(
      int @Interned [] seq,
      @IndexFor("#1") @LessThan("#3") int start,
      @NonNegative @LTLengthOf(value = "#1", offset = "#2 - 1") int end) {
    if (assertsEnabled && !Intern.isInterned(seq)) {
      throw new IllegalArgumentException();
    }
    Subsequence<int @Interned []> sai = new Subsequence<>(seq, start, end);
    WeakReference<int @Interned []> lookup = internedIntSubsequence.get(sai);
    int[] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      int[] subseqUninterned = ArraysPlume.subarray(seq, start, end - start);
      int @Interned [] subseq = Intern.intern(subseqUninterned);
      internedIntSubsequence.put(sai, new WeakReference<>(subseq));
      return subseq;
    }
  }

  /**
   * Returns a subsequence of seq from start to end that is interned.
   *
   * @param seq the interned sequence whose subsequence should be computed and interned
   * @param start the index of the start of the subsequence to compute and intern
   * @param end the index of the end of the subsequence to compute and intern
   * @return a subsequence of seq from start to end that is interned
   * @see #internSubsequence(int[], int, int)
   */
  @SuppressWarnings({"allcheckers:purity", "lock"}) // interning logic
  @Pure
  public static long @Interned [] internSubsequence(
      long @Interned [] seq,
      @IndexFor("#1") @LessThan("#3") int start,
      @NonNegative @LTLengthOf(value = "#1", offset = "#2 - 1") int end) {
    if (assertsEnabled && !Intern.isInterned(seq)) {
      throw new IllegalArgumentException();
    }
    Subsequence<long @Interned []> sai = new Subsequence<>(seq, start, end);
    WeakReference<long @Interned []> lookup = internedLongSubsequence.get(sai);
    long[] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      long[] subseq_uninterned = ArraysPlume.subarray(seq, start, end - start);
      long @Interned [] subseq = Intern.intern(subseq_uninterned);
      internedLongSubsequence.put(sai, new WeakReference<>(subseq));
      return subseq;
    }
  }

  /**
   * Returns a subsequence of seq from start to end that is interned.
   *
   * @param seq the interned sequence whose subsequence should be computed and interned
   * @param start the index of the start of the subsequence to compute and intern
   * @param end the index of the end of the subsequence to compute and intern
   * @return a subsequence of seq from start to end that is interned
   * @see #internSubsequence(int[], int, int)
   */
  @SuppressWarnings({"allcheckers:purity", "lock"}) // interning logic
  @Pure
  public static double @Interned [] internSubsequence(
      double @Interned [] seq,
      @IndexFor("#1") @LessThan("#3") int start,
      @NonNegative @LTLengthOf(value = "#1", offset = "#2 - 1") int end) {
    if (assertsEnabled && !Intern.isInterned(seq)) {
      throw new IllegalArgumentException();
    }
    Subsequence<double @Interned []> sai = new Subsequence<>(seq, start, end);
    WeakReference<double @Interned []> lookup = internedDoubleSubsequence.get(sai);
    double[] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      double[] subseq_uninterned = ArraysPlume.subarray(seq, start, end - start);
      double @Interned [] subseq = Intern.intern(subseq_uninterned);
      internedDoubleSubsequence.put(sai, new WeakReference<>(subseq));
      return subseq;
    }
  }

  /**
   * Returns a subsequence of seq from start to end that is interned.
   *
   * @param seq the interned sequence whose subsequence should be computed and interned
   * @param start the index of the start of the subsequence to compute and intern
   * @param end the index of the end of the subsequence to compute and intern
   * @return a subsequence of seq from start to end that is interned
   * @see #internSubsequence(int[], int, int)
   */
  @SuppressWarnings({"allcheckers:purity", "lock"}) // interning logic
  @Pure
  public static @PolyNull @Interned Object @Interned [] internSubsequence(
      @PolyNull @Interned Object @Interned [] seq,
      @IndexFor("#1") @LessThan("#3") int start,
      @NonNegative @LTLengthOf(value = "#1", offset = "#2 - 1") int end) {
    if (assertsEnabled && !Intern.isInterned(seq)) {
      throw new IllegalArgumentException();
    }
    Subsequence<@PolyNull @Interned Object @Interned []> sai =
        new Subsequence<@PolyNull @Interned Object @Interned []>(seq, start, end);
    @SuppressWarnings("nullness") // same nullness as key
    WeakReference<@PolyNull @Interned Object @Interned []> lookup =
        internedObjectSubsequence.get(sai);
    @PolyNull @Interned Object[] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @PolyNull @Interned Object[] subseq_uninterned = ArraysPlume.subarray(seq, start, end - start);
      @PolyNull @Interned Object @Interned [] subseq = Intern.intern(subseq_uninterned);
      @SuppressWarnings({"nullness", "UnusedVariable"}) // safe because map does no side effects
      Object
          ignore = // assignment just so there is a place to hang the @SuppressWarnings annotation
          internedObjectSubsequence.put(sai, new WeakReference<>(subseq));
      return subseq;
    }
  }

  /**
   * Returns a subsequence of seq from start to end that is interned.
   *
   * @param seq the interned sequence whose subsequence should be computed and interned
   * @param start the index of the start of the subsequence to compute and intern
   * @param end the index of the end of the subsequence to compute and intern
   * @return a subsequence of seq from start to end that is interned
   * @see #internSubsequence(int[], int, int)
   */
  @Pure
  @SuppressWarnings({"allcheckers:purity", "lock"}) // interning logic
  public static @PolyNull @Interned String @Interned [] internSubsequence(
      @PolyNull @Interned String @Interned [] seq,
      @IndexFor("#1") @LessThan("#3") int start,
      @NonNegative @LTLengthOf(value = "#1", offset = "#2 - 1") int end) {
    if (assertsEnabled && !Intern.isInterned(seq)) {
      throw new IllegalArgumentException();
    }
    Subsequence<@PolyNull @Interned String @Interned []> sai =
        new Subsequence<@PolyNull @Interned String @Interned []>(seq, start, end);
    @SuppressWarnings("nullness") // same nullness as key
    WeakReference<@PolyNull @Interned String @Interned []> lookup =
        internedStringSubsequence.get(sai);
    @PolyNull @Interned String[] result1 = (lookup != null) ? lookup.get() : null;
    if (result1 != null) {
      return result1;
    } else {
      @PolyNull @Interned String[] subseq_uninterned = ArraysPlume.subarray(seq, start, end - start);
      @PolyNull @Interned String @Interned [] subseq = Intern.intern(subseq_uninterned);
      @SuppressWarnings({"nullness", "UnusedVariable"}) // safe because map does no side effects
      Object
          ignore = // assignment just so there is a place to hang the @SuppressWarnings annotation
          internedStringSubsequence.put(sai, new WeakReference<>(subseq));
      return subseq;
    }
  }

  /**
   * A subsequence view on a sequence. Actually, this imposes no semantics. It just has 3 fields: an
   * interned sequence, a start index, and an end index. Requires that the sequence be interned.
   * Used for interning the repeated finding of subsequences on the same sequence.
   */
  private static final class Subsequence<T extends @Interned Object> {
    /** The full sequence. The Subsequence object represents part of this sequence. */
    public T seq;
    /** The start index, inclusive. */
    public @NonNegative int start;
    // TODO: inclusive or exclusive?
    /** The end index. */
    public int end;

    /**
     * Creates a subsequence view.
     *
     * @param seq an interned array
     * @param start the start index
     * @param end the end index
     */
    public Subsequence(T seq, @NonNegative int start, int end) {
      if (assertsEnabled && !Intern.isInterned(seq)) {
        throw new IllegalArgumentException();
      }
      this.seq = seq;
      this.start = start;
      this.end = end;
    }

    @SuppressWarnings("unchecked")
    @Pure
    @Override
    public boolean equals(
        @GuardSatisfied Subsequence<T> this, @GuardSatisfied @Nullable Object other) {
      if (other instanceof Subsequence<?>) {
        @SuppressWarnings("unchecked")
        Subsequence<T> otherSai = (Subsequence<T>) other;
        return equalsSubsequence(otherSai);
      } else {
        return false;
      }
    }

    /**
     * Returns true if this object equals the given one.
     *
     * @param other the sequence to compare to
     * @return true if this object equals {@code other}
     */
    @Pure
    public boolean equalsSubsequence(
        @GuardSatisfied Subsequence<T> this, @GuardSatisfied Subsequence<T> other) {
      return ((this.seq == other.seq) && this.start == other.start && this.end == other.end);
    }

    @Pure
    @Override
    public int hashCode(@GuardSatisfied Subsequence<T> this) {
      return seq.hashCode() + start * 30 - end * 2;
    }

    // For debugging
    @SideEffectFree
    @Override
    public String toString(@GuardSatisfied Subsequence<T> this) {
      return "SAI(" + start + "," + end + ") from: " + ArraysPlume.toString(seq);
    }
  }

  /**
   * Hasher object which hashes and compares String[] objects according to their contents.
   *
   * @see Hasher
   */
  private static final class SubsequenceHasher<T extends @Interned Object> implements Hasher {
    /** Create a new SubsequenceHasher. */
    public SubsequenceHasher() {}

    @Override
    public boolean equals(Object a1, Object a2) {
      @SuppressWarnings("unchecked")
      Subsequence<T> sai1 = (Subsequence<T>) a1;
      @SuppressWarnings("unchecked")
      Subsequence<T> sai2 = (Subsequence<T>) a2;
      // The SAI objects are *not* interned, but the arrays inside them are.
      return sai1.equals(sai2);
    }

    @Override
    public int hashCode(Object o) {
      return o.hashCode();
    }
  }
}
