package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class StringBuffer extends AbstractStringBuilder implements java.io.Serializable, CharSequence{
  static final long serialVersionUID = 0;
  public StringBuffer() { throw new RuntimeException("skeleton method"); }
  public StringBuffer(int a1) { throw new RuntimeException("skeleton method"); }
  public StringBuffer(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public StringBuffer(java.lang.CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int length() { throw new RuntimeException("skeleton method"); }
  public synchronized int capacity() { throw new RuntimeException("skeleton method"); }
  public synchronized void ensureCapacity(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void trimToSize() { throw new RuntimeException("skeleton method"); }
  public synchronized void setLength(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized char charAt(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int codePointAt(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int codePointBefore(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int codePointCount(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized int offsetByCodePoints(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized void getChars(int a1, int a2, char[] a3, int a4) { throw new RuntimeException("skeleton method"); }
  public synchronized void setCharAt(int a1, char a2) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer append(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer append(@Nullable java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer append(@Nullable java.lang.StringBuffer a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuffer append(@Nullable java.lang.CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer append(@Nullable java.lang.CharSequence a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer append(char[] a1) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer append(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer append(boolean a1) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer append(char a1) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer append(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer appendCodePoint(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer append(long a1) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer append(float a1) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer append(double a1) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer delete(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer deleteCharAt(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer replace(int a1, int a2, java.lang.String a3) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.String substring(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.CharSequence subSequence(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.String substring(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer insert(int a1, char[] a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer insert(int a1, @Nullable java.lang.Object a2) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer insert(int a1, @Nullable java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer insert(int a1, char[] a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuffer insert(int a1, @Nullable java.lang.CharSequence a2) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer insert(int a1, @Nullable java.lang.CharSequence a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuffer insert(int a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer insert(int a1, char a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuffer insert(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuffer insert(int a1, long a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuffer insert(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuffer insert(int a1, double a2) { throw new RuntimeException("skeleton method"); }
  public int indexOf(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int indexOf(java.lang.String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int lastIndexOf(java.lang.String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.StringBuffer reverse() { throw new RuntimeException("skeleton method"); }
  public synchronized java.lang.String toString() { throw new RuntimeException("skeleton method"); }
}
