package java.lang;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;


public final class Integer extends Number implements Comparable<Integer> {
  private static final long serialVersionUID = 0;
  public final static int MIN_VALUE = -2147483648;
  public final static int MAX_VALUE = 2147483647;
  public final static Class<Integer> TYPE = null;
  public final static int SIZE = 32;
  @SideEffectFree public static String toString(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String toHexString(int a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String toOctalString(int a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String toBinaryString(int a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String toString(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure static int stringSize(int x) { throw new RuntimeException("skeleton method"); }
  @Pure public static int parseInt(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public static int parseInt(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Integer valueOf(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Integer valueOf(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Integer valueOf(int a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Integer(int a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Integer(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public byte byteValue() { throw new RuntimeException("skeleton method"); }
  @Pure public short shortValue() { throw new RuntimeException("skeleton method"); }
  @Pure public int intValue() { throw new RuntimeException("skeleton method"); }
  @Pure public long longValue() { throw new RuntimeException("skeleton method"); }
  @Pure public float floatValue() { throw new RuntimeException("skeleton method"); }
  @Pure public double doubleValue() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static @Nullable Integer getInteger(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Integer getInteger(@Nullable String a1, int a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static @PolyNull Integer getInteger(@Nullable String a1, @PolyNull Integer a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Integer decode(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public int compareTo(Integer a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static int highestOneBit(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static int lowestOneBit(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static int numberOfLeadingZeros(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static int numberOfTrailingZeros(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static int bitCount(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static int rotateLeft(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  @Pure public static int rotateRight(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  @Pure public static int reverse(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static int signum(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static int reverseBytes(int a1) { throw new RuntimeException("skeleton method"); }
}
