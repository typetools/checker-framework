package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Integer extends Number implements Comparable<Integer> {
  private static final long serialVersionUID = 0;
  public final static int MIN_VALUE = -2147483648;
  public final static int MAX_VALUE = 2147483647;
  public final static Class<Integer> TYPE;
  public final static int SIZE = 32;
  public static String toString(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static String toHexString(int a1) { throw new RuntimeException("skeleton method"); }
  public static String toOctalString(int a1) { throw new RuntimeException("skeleton method"); }
  public static String toBinaryString(int a1) { throw new RuntimeException("skeleton method"); }
  public static String toString(int a1) { throw new RuntimeException("skeleton method"); }
  static int stringSize(int x) { throw new RuntimeException("skeleton method"); }
  public static int parseInt(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static int parseInt(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static Integer valueOf(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static Integer valueOf(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static Integer valueOf(int a1) { throw new RuntimeException("skeleton method"); }
  public Integer(int a1) { throw new RuntimeException("skeleton method"); }
  public Integer(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public byte byteValue() { throw new RuntimeException("skeleton method"); }
  public short shortValue() { throw new RuntimeException("skeleton method"); }
  public int intValue() { throw new RuntimeException("skeleton method"); }
  public long longValue() { throw new RuntimeException("skeleton method"); }
  public float floatValue() { throw new RuntimeException("skeleton method"); }
  public double doubleValue() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public static @Nullable Integer getInteger(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  public static Integer getInteger(@Nullable String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static @PolyNull Integer getInteger(@Nullable String a1, @PolyNull Integer a2) { throw new RuntimeException("skeleton method"); }
  public static Integer decode(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public int compareTo(Integer a1) { throw new RuntimeException("skeleton method"); }
  public static int highestOneBit(int a1) { throw new RuntimeException("skeleton method"); }
  public static int lowestOneBit(int a1) { throw new RuntimeException("skeleton method"); }
  public static int numberOfLeadingZeros(int a1) { throw new RuntimeException("skeleton method"); }
  public static int numberOfTrailingZeros(int a1) { throw new RuntimeException("skeleton method"); }
  public static int bitCount(int a1) { throw new RuntimeException("skeleton method"); }
  public static int rotateLeft(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static int rotateRight(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static int reverse(int a1) { throw new RuntimeException("skeleton method"); }
  public static int signum(int a1) { throw new RuntimeException("skeleton method"); }
  public static int reverseBytes(int a1) { throw new RuntimeException("skeleton method"); }
}
