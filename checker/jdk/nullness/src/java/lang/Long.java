package java.lang;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;


public final class Long extends Number implements Comparable<Long> {
  private static final long serialVersionUID = 0;
  public final static long MIN_VALUE = -9223372036854775808L;
  public final static long MAX_VALUE = 9223372036854775807L;
  public final static Class<Long> TYPE = null;
  public final static int SIZE = 64;
  @SideEffectFree public static String toString(long a1, int a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String toHexString(long a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String toOctalString(long a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String toBinaryString(long a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String toString(long a1) { throw new RuntimeException("skeleton method"); }
  @Pure static int stringSize(long x) { throw new RuntimeException("skeleton method"); }
  @Pure public static long parseLong(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public static long parseLong(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Long valueOf(String a1, int a2) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Long valueOf(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Long valueOf(long a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Long decode(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Long(long a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public Long(String a1) throws NumberFormatException { throw new RuntimeException("skeleton method"); }
  @Pure public byte byteValue() { throw new RuntimeException("skeleton method"); }
  @Pure public short shortValue() { throw new RuntimeException("skeleton method"); }
  @Pure public int intValue() { throw new RuntimeException("skeleton method"); }
  @Pure public long longValue() { throw new RuntimeException("skeleton method"); }
  @Pure public float floatValue() { throw new RuntimeException("skeleton method"); }
  @Pure public double doubleValue() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toString() { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static @Nullable Long getLong(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static Long getLong(@Nullable String a1, long a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static @PolyNull Long getLong(@Nullable String a1, @PolyNull Long a2) { throw new RuntimeException("skeleton method"); }
  @Pure public int compareTo(Long a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static long highestOneBit(long a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static long lowestOneBit(long a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static int numberOfLeadingZeros(long a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static int numberOfTrailingZeros(long a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static int bitCount(long a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static long rotateLeft(long a1, int a2) { throw new RuntimeException("skeleton method"); }
  @Pure public static long rotateRight(long a1, int a2) { throw new RuntimeException("skeleton method"); }
  @Pure public static long reverse(long a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static int signum(long a1) { throw new RuntimeException("skeleton method"); }
  @Pure public static long reverseBytes(long a1) { throw new RuntimeException("skeleton method"); }
}
