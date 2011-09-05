package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class String implements java.io.Serializable, Comparable<String>, CharSequence {
  private static final long serialVersionUID = 0;
  public final static java.util.Comparator<String> CASE_INSENSITIVE_ORDER;
  public String() { throw new RuntimeException("skeleton method"); }
  public String(String a1) { throw new RuntimeException("skeleton method"); }
  public String(char[] a1) { throw new RuntimeException("skeleton method"); }
  public String(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public String(int[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, int a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, int a2) { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, int a2, int a3, String a4) throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, int a2, int a3, java.nio.charset.Charset a4) { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, String a2) throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, java.nio.charset.Charset a2) { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public String(byte[] a1) { throw new RuntimeException("skeleton method"); }
  public String(StringBuffer a1) { throw new RuntimeException("skeleton method"); }
  public String(StringBuilder a1) { throw new RuntimeException("skeleton method"); }
  public int length() { throw new RuntimeException("skeleton method"); }
  public boolean isEmpty() { throw new RuntimeException("skeleton method"); }
  public char charAt(int a1) { throw new RuntimeException("skeleton method"); }
  public int codePointAt(int a1) { throw new RuntimeException("skeleton method"); }
  public int codePointBefore(int a1) { throw new RuntimeException("skeleton method"); }
  public int codePointCount(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int offsetByCodePoints(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public void getChars(int a1, int a2, char[] a3, int a4) { throw new RuntimeException("skeleton method"); }
  public void getBytes(int a1, int a2, byte[] a3, int a4) { throw new RuntimeException("skeleton method"); }
  public byte[] getBytes(String a1) throws java.io.UnsupportedEncodingException { throw new RuntimeException("skeleton method"); }
  public byte[] getBytes(java.nio.charset.Charset a1) { throw new RuntimeException("skeleton method"); }
  public byte[] getBytes() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public boolean contentEquals(StringBuffer a1) { throw new RuntimeException("skeleton method"); }
  public boolean contentEquals(CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public boolean equalsIgnoreCase(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(String a1) { throw new RuntimeException("skeleton method"); }
  public int compareToIgnoreCase(String a1) { throw new RuntimeException("skeleton method"); }
  public boolean regionMatches(int a1, String a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public boolean regionMatches(boolean a1, int a2, String a3, int a4, int a5) { throw new RuntimeException("skeleton method"); }
  public boolean startsWith(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public boolean startsWith(String a1) { throw new RuntimeException("skeleton method"); }
  public boolean endsWith(String a1) { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public int indexOf(int a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(int a1) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int indexOf(String a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(String a1) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public String substring(int a1) { throw new RuntimeException("skeleton method"); }
  public String substring(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public CharSequence subSequence(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public String concat(String a1) { throw new RuntimeException("skeleton method"); }
  public String replace(char a1, char a2) { throw new RuntimeException("skeleton method"); }
  public boolean matches(String a1) { throw new RuntimeException("skeleton method"); }
  public boolean contains(CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public String replaceFirst(String a1, String a2) { throw new RuntimeException("skeleton method"); }
  public String replaceAll(String a1, String a2) { throw new RuntimeException("skeleton method"); }
  public String replace(CharSequence a1, CharSequence a2) { throw new RuntimeException("skeleton method"); }
  public String[] split(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public String[] split(String a1) { throw new RuntimeException("skeleton method"); }
  public String toLowerCase(java.util.Locale a1) { throw new RuntimeException("skeleton method"); }
  public String toLowerCase() { throw new RuntimeException("skeleton method"); }
  public String toUpperCase(java.util.Locale a1) { throw new RuntimeException("skeleton method"); }
  public String toUpperCase() { throw new RuntimeException("skeleton method"); }
  public String trim() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
  public char[] toCharArray() { throw new RuntimeException("skeleton method"); }
  public static String format(String a1, @Nullable Object... a2) { throw new RuntimeException("skeleton method"); }
  public static String format(@Nullable java.util.Locale a1, String a2, @Nullable Object... a3) { throw new RuntimeException("skeleton method"); }
  public static String valueOf(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public static String valueOf(char[] a1) { throw new RuntimeException("skeleton method"); }
  public static String valueOf(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public static String copyValueOf(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public static String copyValueOf(char[] a1) { throw new RuntimeException("skeleton method"); }
  public static String valueOf(boolean a1) { throw new RuntimeException("skeleton method"); }
  public static String valueOf(char a1) { throw new RuntimeException("skeleton method"); }
  public static String valueOf(int a1) { throw new RuntimeException("skeleton method"); }
  public static String valueOf(long a1) { throw new RuntimeException("skeleton method"); }
  public static String valueOf(float a1) { throw new RuntimeException("skeleton method"); }
  public static String valueOf(double a1) { throw new RuntimeException("skeleton method"); }
  public native String intern();
}
