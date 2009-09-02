package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Integer extends java.lang.Number implements java.lang.Comparable<java.lang.Integer> {
  private static final long serialVersionUID = 0;
  public final static int MIN_VALUE = -2147483648;
  public final static int MAX_VALUE = 2147483647;
  public final static java.lang.Class<java.lang.Integer> TYPE;
  public final static int SIZE = 32;
  public static java.lang.String toString(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String toHexString(int a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String toOctalString(int a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String toBinaryString(int a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String toString(int a1) { throw new RuntimeException("skeleton method"); }
  static int stringSize(int x) { throw new RuntimeException("skeleton method"); }
  public static int parseInt(java.lang.String a1, int a2) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static int parseInt(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static java.lang.Integer valueOf(java.lang.String a1, int a2) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static java.lang.Integer valueOf(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static java.lang.Integer valueOf(int a1) { throw new RuntimeException("skeleton method"); }
  public Integer(int a1) { throw new RuntimeException("skeleton method"); }
  public Integer(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public byte byteValue() { throw new RuntimeException("skeleton method"); }
  public short shortValue() { throw new RuntimeException("skeleton method"); }
  public int intValue() { throw new RuntimeException("skeleton method"); }
  public long longValue() { throw new RuntimeException("skeleton method"); }
  public float floatValue() { throw new RuntimeException("skeleton method"); }
  public double doubleValue() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public static @Nullable java.lang.Integer getInteger(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.Integer getInteger(@Nullable String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static @PolyNull java.lang.Integer getInteger(@Nullable String a1, @PolyNull java.lang.Integer a2) { throw new RuntimeException("skeleton method"); }
  public static java.lang.Integer decode(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public int compareTo(java.lang.Integer a1) { throw new RuntimeException("skeleton method"); }
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
