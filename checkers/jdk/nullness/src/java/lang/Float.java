package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Float extends Number implements Comparable<Float> {
  private static final long serialVersionUID = 0;
  public final static float POSITIVE_INFINITY = 0;
  public final static float NEGATIVE_INFINITY = 0;
  public final static float NaN = 0;
  public final static float MAX_VALUE = 0;
  public final static float MIN_NORMAL = 0;
  public final static float MIN_VALUE = 0;
  public final static int MAX_EXPONENT = 0;
  public final static int MIN_EXPONENT = 0;
  public final static int SIZE = 32;
  public final static Class<Float> TYPE;
  public static String toString(float a1) { throw new RuntimeException("skeleton method"); }
  public static String toHexString(float a1) { throw new RuntimeException("skeleton method"); }
  public static Float valueOf(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static Float valueOf(float a1) { throw new RuntimeException("skeleton method"); }
  public static float parseFloat(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static boolean isNaN(float a1) { throw new RuntimeException("skeleton method"); }
  public static boolean isInfinite(float a1) { throw new RuntimeException("skeleton method"); }
  public Float(float a1) { throw new RuntimeException("skeleton method"); }
  public Float(double a1) { throw new RuntimeException("skeleton method"); }
  public Float(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public boolean isNaN() { throw new RuntimeException("skeleton method"); }
  public boolean isInfinite() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public byte byteValue() { throw new RuntimeException("skeleton method"); }
  public short shortValue() { throw new RuntimeException("skeleton method"); }
  public int intValue() { throw new RuntimeException("skeleton method"); }
  public long longValue() { throw new RuntimeException("skeleton method"); }
  public float floatValue() { throw new RuntimeException("skeleton method"); }
  public double doubleValue() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public static int floatToIntBits(float a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(Float a1) { throw new RuntimeException("skeleton method"); }
  public static int compare(float a1, float a2) { throw new RuntimeException("skeleton method"); }

  public static native float intBitsToFloat(int a1);
  public static native int floatToRawIntBits(float a1);

}
