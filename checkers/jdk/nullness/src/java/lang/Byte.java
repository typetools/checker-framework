package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Byte extends Number implements Comparable<Byte> {
  private static final long serialVersionUID = 0;
  public final static byte MIN_VALUE = -128;
  public final static byte MAX_VALUE = 127;
  public final static Class<Byte> TYPE;
  public final static int SIZE = 8;
  public static String toString(byte a1) { throw new RuntimeException("skeleton method"); }
  public static Byte valueOf(byte a1) { throw new RuntimeException("skeleton method"); }
  public static byte parseByte(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static byte parseByte(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static Byte valueOf(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static Byte valueOf(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public static Byte decode(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public Byte(byte a1) { throw new RuntimeException("skeleton method"); }
  public Byte(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public byte byteValue() { throw new RuntimeException("skeleton method"); }
  public short shortValue() { throw new RuntimeException("skeleton method"); }
  public int intValue() { throw new RuntimeException("skeleton method"); }
  public long longValue() { throw new RuntimeException("skeleton method"); }
  public float floatValue() { throw new RuntimeException("skeleton method"); }
  public double doubleValue() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(Byte a1) { throw new RuntimeException("skeleton method"); }
}
