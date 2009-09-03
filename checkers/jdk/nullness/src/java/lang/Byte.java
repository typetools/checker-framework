package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Byte extends java.lang.Number implements java.lang.Comparable<java.lang.Byte> {
  private static final long serialVersionUID = 0;
  public final static byte MIN_VALUE = -128;
  public final static byte MAX_VALUE = 127;
  public final static java.lang.Class<java.lang.Byte> TYPE;
  public final static int SIZE = 8;
  public static java.lang.String toString(byte a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.Byte valueOf(byte a1) { throw new RuntimeException("skeleton method"); }
  public static byte parseByte(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static byte parseByte(java.lang.String a1, int a2) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static java.lang.Byte valueOf(java.lang.String a1, int a2) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static java.lang.Byte valueOf(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static java.lang.Byte decode(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public Byte(byte a1) { throw new RuntimeException("skeleton method"); }
  public Byte(java.lang.String a1) throws java.lang.NumberFormatException { throw new RuntimeException("skeleton method"); }
  public byte byteValue() { throw new RuntimeException("skeleton method"); }
  public short shortValue() { throw new RuntimeException("skeleton method"); }
  public int intValue() { throw new RuntimeException("skeleton method"); }
  public long longValue() { throw new RuntimeException("skeleton method"); }
  public float floatValue() { throw new RuntimeException("skeleton method"); }
  public double doubleValue() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(java.lang.Byte a1) { throw new RuntimeException("skeleton method"); }
}
