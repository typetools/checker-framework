package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class String implements java.io.Serializable, java.lang.Comparable<java.lang.String>, java.lang.CharSequence {
  private static final long serialVersionUID = 0;
  public final static java.util.Comparator<java.lang.String> CASE_INSENSITIVE_ORDER;
  public String() { throw new RuntimeException("skeleton method"); }
  public String(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public String(char[] a1) { throw new RuntimeException("skeleton method"); }
  public String(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public String(int[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, int a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, int a2) { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, int a2, int a3, java.lang.String a4) throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, int a2, int a3, java.nio.charset.Charset a4) { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, java.lang.String a2) throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, java.nio.charset.Charset a2) { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1) { throw new RuntimeException("skeleton method"); }
  public String(java.lang.StringBuffer a1) { throw new RuntimeException("skeleton method"); }
  public String(java.lang.StringBuilder a1) { throw new RuntimeException("skeleton method"); }
  public int length() { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public char charAt(int a1) { throw new RuntimeException("skeleton method"); }
  public int codePointAt(int a1) { throw new RuntimeException("skeleton method"); }
  public int codePointBefore(int a1) { throw new RuntimeException("skeleton method"); }
  public int codePointCount(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int offsetByCodePoints(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void getChars(int a1, int a2, char[] a3, int a4) { throw new RuntimeException("skeleton method"); }
  public void getBytes(int a1, int a2, byte[] a3, int a4) { throw new RuntimeException("skeleton method"); }
  public byte[] getBytes(java.lang.String a1) throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public byte[] getBytes(java.nio.charset.Charset a1) { throw new RuntimeException("skeleton method"); }
  public byte[] getBytes() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean contentEquals(java.lang.StringBuffer a1) { throw new RuntimeException("skeleton method"); }
  public boolean contentEquals(java.lang.CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public boolean equalsIgnoreCase(@Nullable java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public int compareToIgnoreCase(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public boolean regionMatches(int a1, java.lang.String a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public boolean regionMatches(boolean a1, int a2, java.lang.String a3, int a4, int a5) { throw new RuntimeException("skeleton method"); }
  public boolean startsWith(java.lang.String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public boolean startsWith(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public boolean endsWith(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public int indexOf(int a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(int a1) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int indexOf(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(java.lang.String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(java.lang.String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.String substring(int a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String substring(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.CharSequence subSequence(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.String concat(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String replace(char a1, char a2) { throw new RuntimeException("skeleton method"); }
  public boolean matches(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public boolean contains(java.lang.CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String replaceFirst(java.lang.String a1, java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.String replaceAll(java.lang.String a1, java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.String replace(java.lang.CharSequence a1, java.lang.CharSequence a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.String[] split(java.lang.String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.String[] split(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String toLowerCase(java.util.Locale a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String toLowerCase() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toUpperCase(java.util.Locale a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String toUpperCase() { throw new RuntimeException("skeleton method"); }
  public java.lang.String trim() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public char[] toCharArray() { throw new RuntimeException("skeleton method"); }
  public static java.lang.String format(java.lang.String a1, @Nullable java.lang.Object... a2) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String format(@Nullable java.util.Locale a1, java.lang.String a2, @Nullable java.lang.Object... a3) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String valueOf(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String valueOf(char[] a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String valueOf(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String copyValueOf(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String copyValueOf(char[] a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String valueOf(boolean a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String valueOf(char a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String valueOf(int a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String valueOf(long a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String valueOf(float a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String valueOf(double a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String intern() { throw new RuntimeException("skeleton method"); }
}
