package java.lang;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;


public final class Short extends Number implements Comparable<Short> {
  private static final long serialVersionUID = 0;
  public final static short MIN_VALUE = -32768;
  public final static short MAX_VALUE = 32767;
  public final static Class<Short> TYPE = null;
  public final static int SIZE = 16;
  @SideEffectFree public static String toString(short a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static short parseShort(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public static short parseShort(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Short valueOf(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Short valueOf(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Short valueOf(short a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Short decode(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Short(short a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Short(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public byte byteValue() { throw new RuntimeException("skeleton method"); }
  @Pure public short shortValue() { throw new RuntimeException("skeleton method"); }
  @Pure public int intValue() { throw new RuntimeException("skeleton method"); }
  @Pure public long longValue() { throw new RuntimeException("skeleton method"); }
  @Pure public float floatValue() { throw new RuntimeException("skeleton method"); }
  @Pure public double doubleValue() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int compareTo(Short a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static short reverseBytes(short a1) { throw new RuntimeException("skeleton method"); }
}
