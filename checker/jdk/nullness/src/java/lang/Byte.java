package java.lang;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;


public final class Byte extends Number implements Comparable<Byte> {
  private static final long serialVersionUID = 0;
  public final static byte MIN_VALUE = -128;
  public final static byte MAX_VALUE = 127;
  public final static Class<Byte> TYPE = null;
  public final static int SIZE = 8;
  @SideEffectFree public static String toString(byte a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static Byte valueOf(byte a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static byte parseByte(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public static byte parseByte(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public static Byte valueOf(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public static Byte valueOf(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public static Byte decode(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  public Byte(byte a1) { throw new RuntimeException("skeleton method"); }
  public Byte(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public byte byteValue() { throw new RuntimeException("skeleton method"); }
  @Pure public short shortValue() { throw new RuntimeException("skeleton method"); }
  @Pure public int intValue() { throw new RuntimeException("skeleton method"); }
  @Pure public long longValue() { throw new RuntimeException("skeleton method"); }
  @Pure public float floatValue() { throw new RuntimeException("skeleton method"); }
  @Pure public double doubleValue() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int compareTo(Byte a1) { throw new RuntimeException("skeleton method"); }
}
