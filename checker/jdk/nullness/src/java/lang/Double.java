package java.lang;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;


public final class Double extends Number implements Comparable<Double> {
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
  public final static Class<Double> TYPE = Double.class;
  @SideEffectFree public static String toString(double a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String toHexString(double a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Double valueOf(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Double valueOf(double a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static double parseDouble(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isNaN(double a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static boolean isInfinite(double a1) { throw new RuntimeException("skeleton method"); }
  public Double(double a1) { throw new RuntimeException("skeleton method"); }
  public Double(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isNaN() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isInfinite() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  @Pure public byte byteValue() { throw new RuntimeException("skeleton method"); }
  @Pure public short shortValue() { throw new RuntimeException("skeleton method"); }
  @Pure public int intValue() { throw new RuntimeException("skeleton method"); }
  @Pure public long longValue() { throw new RuntimeException("skeleton method"); }
  @Pure public float floatValue() { throw new RuntimeException("skeleton method"); }
  @Pure public double doubleValue() { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static long doubleToLongBits(double a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int compareTo(Double a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static int compare(double a1, double a2) { throw new RuntimeException("skeleton method"); }

  @Pure public static native double longBitsToDouble(long bits);
  @Pure public static native long doubleToRawLongBits(double value);

}
