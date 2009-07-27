package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Long extends java.lang.Number implements java.lang.Comparable<java.lang.Long> {
  private static final long serialVersionUID = 0;
  public final static long MIN_VALUE = -9223372036854775808L;
  public final static long MAX_VALUE = 9223372036854775807L;
  public final static java.lang.Class<java.lang.Long> TYPE;
  public final static int SIZE = 64;
  public static java.lang.String toString(long a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String toHexString(long a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String toOctalString(long a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String toBinaryString(long a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String toString(long a1) { throw new RuntimeException("skeleton method"); }
  static int stringSize(long x) { throw new RuntimeException("skeleton method"); }
  public static long parseLong(java.lang.String a1, int a2) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static long parseLong(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static java.lang.Long valueOf(java.lang.String a1, int a2) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static java.lang.Long valueOf(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static java.lang.Long valueOf(long a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.Long decode(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public Long(long a1) { throw new RuntimeException("skeleton method"); }
  public Long(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public byte byteValue() { throw new RuntimeException("skeleton method"); }
  public short shortValue() { throw new RuntimeException("skeleton method"); }
  public int intValue() { throw new RuntimeException("skeleton method"); }
  public long longValue() { throw new RuntimeException("skeleton method"); }
  public float floatValue() { throw new RuntimeException("skeleton method"); }
  public double doubleValue() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public static @Nullable java.lang.Long getLong(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.Long getLong(@Nullable String a1, long a2) { throw new RuntimeException("skeleton method"); }
  public static @PolyNull java.lang.Long getLong(@Nullable String a1, @PolyNull java.lang.Long a2) { throw new RuntimeException("skeleton method"); }
  public int compareTo(java.lang.Long a1) { throw new RuntimeException("skeleton method"); }
  public static long highestOneBit(long a1) { throw new RuntimeException("skeleton method"); }
  public static long lowestOneBit(long a1) { throw new RuntimeException("skeleton method"); }
  public static int numberOfLeadingZeros(long a1) { throw new RuntimeException("skeleton method"); }
  public static int numberOfTrailingZeros(long a1) { throw new RuntimeException("skeleton method"); }
  public static int bitCount(long a1) { throw new RuntimeException("skeleton method"); }
  public static long rotateLeft(long a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static long rotateRight(long a1, int a2) { throw new RuntimeException("skeleton method"); }
  public static long reverse(long a1) { throw new RuntimeException("skeleton method"); }
  public static int signum(long a1) { throw new RuntimeException("skeleton method"); }
  public static long reverseBytes(long a1) { throw new RuntimeException("skeleton method"); }
}
