package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Short extends Number implements Comparable<Short> {
  private static final long serialVersionUID = 0;
  public final static short MIN_VALUE = -32768;
  public final static short MAX_VALUE = 32767;
  public final static Class<Short> TYPE;
  public final static int SIZE = 16;
  public static String toString(short a1) { throw new RuntimeException("skeleton method"); }
  public static short parseShort(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static short parseShort(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static Short valueOf(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static Short valueOf(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static Short valueOf(short a1) { throw new RuntimeException("skeleton method"); }
  public static Short decode(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public Short(short a1) { throw new RuntimeException("skeleton method"); }
  public Short(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public byte byteValue() { throw new RuntimeException("skeleton method"); }
  public short shortValue() { throw new RuntimeException("skeleton method"); }
  public int intValue() { throw new RuntimeException("skeleton method"); }
  public long longValue() { throw new RuntimeException("skeleton method"); }
  public float floatValue() { throw new RuntimeException("skeleton method"); }
  public double doubleValue() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(Short a1) { throw new RuntimeException("skeleton method"); }
  public static short reverseBytes(short a1) { throw new RuntimeException("skeleton method"); }
}
