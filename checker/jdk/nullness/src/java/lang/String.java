package java.lang;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;


public final class String implements java.io.Serializable, Comparable<String>, CharSequence {
  private static final long serialVersionUID = 0;
  public final static java.util.Comparator<String> CASE_INSENSITIVE_ORDER = null;
  @SideEffectFree public String() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(String a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(char[] a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(int[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(byte[] a1, int a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(byte[] a1, int a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(byte[] a1, int a2, int a3, String a4) throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(byte[] a1, int a2, int a3, java.nio.charset.Charset a4) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(byte[] a1, String a2) throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(byte[] a1, java.nio.charset.Charset a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(byte[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(byte[] a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(StringBuffer a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String(StringBuilder a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int length() { throw new RuntimeException("skeleton method"); }
  @Pure public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  @Pure public char charAt(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int codePointAt(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int codePointBefore(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int codePointCount(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  @Pure public int offsetByCodePoints(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void getChars(int a1, int a2, char[] a3, int a4) { throw new RuntimeException("skeleton method"); }
  public void getBytes(int a1, int a2, byte[] a3, int a4) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public byte[] getBytes(String a1) throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public byte[] getBytes(java.nio.charset.Charset a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public byte[] getBytes() { throw new RuntimeException("skeleton method"); }
  @EnsuresNonNullIf(expression="#1", result=true)
  @Pure public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean contentEquals(StringBuffer a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean contentEquals(CharSequence a1) { throw new RuntimeException("skeleton method"); }
  @EnsuresNonNullIf(expression="#1", result=true)
  @Pure public boolean equalsIgnoreCase(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int compareTo(String a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int compareToIgnoreCase(String a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean regionMatches(int a1, String a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean regionMatches(boolean a1, int a2, String a3, int a4, int a5) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean startsWith(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean startsWith(String a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean endsWith(String a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int hashCode() { throw new RuntimeException("skeleton method"); }
  @Pure public int indexOf(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int indexOf(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  @Pure public int lastIndexOf(int a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int lastIndexOf(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  @Pure public int indexOf(String a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int indexOf(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  @Pure public int lastIndexOf(String a1) { throw new RuntimeException("skeleton method"); }
  @Pure public int lastIndexOf(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String substring(int a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String substring(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public CharSequence subSequence(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String concat(String a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String replace(char a1, char a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public boolean matches(String a1) { throw new RuntimeException("skeleton method"); }
  @Pure public boolean contains(CharSequence a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String replaceFirst(String a1, String a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String replaceAll(String a1, String a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String replace(CharSequence a1, CharSequence a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String[] split(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String[] split(String a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toLowerCase(java.util.Locale a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toLowerCase() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toUpperCase(java.util.Locale a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String toUpperCase() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public String trim() { throw new RuntimeException("skeleton method"); }
  @Pure public String toString() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public char[] toCharArray() { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String format(String a1, @Nullable Object... a2) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String format(java.util. @Nullable Locale a1, String a2, @Nullable Object... a3) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String valueOf(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String valueOf(char[] a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String valueOf(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String copyValueOf(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String copyValueOf(char[] a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String valueOf(boolean a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String valueOf(char a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String valueOf(int a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String valueOf(long a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String valueOf(float a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public static String valueOf(double a1) { throw new RuntimeException("skeleton method"); }
  @SideEffectFree public native String intern();
}
