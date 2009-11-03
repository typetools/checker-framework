package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Double extends java.lang.Number implements java.lang.Comparable<java.lang.Double> {
  private static final long serialVersionUID = 0;
  public final static double POSITIVE_INFINITY = 1.0 / 0.0;
  public final static double NEGATIVE_INFINITY = -1.0 / 0.0;
  public final static double NaN = 0.0d / 0.0;
  public final static double MAX_VALUE = 1.7976931348623157E308;
  public final static double MIN_NORMAL = 2.2250738585072014E-308;
  public final static double MIN_VALUE = 4.9E-324;
  public final static int MAX_EXPONENT = 1023;
  public final static int MIN_EXPONENT = -1022;
  public final static int SIZE = 64;
  public final static java.lang.Class<java.lang.Double> TYPE = Double.class;
  public static java.lang.String toString(double a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String toHexString(double a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.Double valueOf(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static java.lang.Double valueOf(double a1) { throw new RuntimeException("skeleton method"); }
  public static double parseDouble(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static boolean isNaN(double a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isInfinite(double a1) { throw new RuntimeException("skeleton method"); }
  public Double(double a1) { throw new RuntimeException("skeleton method"); }
  public Double(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public boolean isNaN() { throw new RuntimeException("skeleton method"); }
  public boolean isInfinite() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public byte byteValue() { throw new RuntimeException("skeleton method"); }
  public short shortValue() { throw new RuntimeException("skeleton method"); }
  public int intValue() { throw new RuntimeException("skeleton method"); }
  public long longValue() { throw new RuntimeException("skeleton method"); }
  public float floatValue() { throw new RuntimeException("skeleton method"); }
  public double doubleValue() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public static long doubleToLongBits(double a1) { throw new RuntimeException("skeleton method"); }
  public static long doubleToRawLongBits(double value) { throw new RuntimeException("skeleton method"); }
  public static double longBitsToDouble(long bits) { throw new RuntimeException("skeleton method"); }
  public int compareTo(java.lang.Double a1) { throw new RuntimeException("skeleton method"); }
  public static int compare(double a1, double a2) { throw new RuntimeException("skeleton method"); }

  public static native double longBitsToDouble(long a1);
  public static native long doubleToRawLongBits(double a1);

}
