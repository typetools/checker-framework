package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class StringBuffer extends AbstractStringBuilder implements java.io.Serializable, CharSequence{
  static final long serialVersionUID = 0;
  public StringBuffer() { throw new RuntimeException("skeleton method"); }
  public StringBuffer(int a1) { throw new RuntimeException("skeleton method"); }
  public StringBuffer(String a1) { throw new RuntimeException("skeleton method"); }
  public StringBuffer(CharSequence a1) { throw new RuntimeException("skeleton method"); }
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
  public synchronized StringBuffer append(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer append(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer append(@Nullable StringBuffer a1) { throw new RuntimeException("skeleton method"); }
  public StringBuffer append(@Nullable CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer append(@Nullable CharSequence a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer append(char[] a1) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer append(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer append(boolean a1) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer append(char a1) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer append(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer appendCodePoint(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer append(long a1) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer append(float a1) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer append(double a1) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer delete(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer deleteCharAt(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer replace(int a1, int a2, String a3) { throw new RuntimeException("skeleton method"); }
  public synchronized String substring(int a1) { throw new RuntimeException("skeleton method"); }
  public synchronized CharSequence subSequence(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized String substring(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer insert(int a1, char[] a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer insert(int a1, @Nullable Object a2) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer insert(int a1, @Nullable String a2) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer insert(int a1, char[] a2) { throw new RuntimeException("skeleton method"); }
  public StringBuffer insert(int a1, @Nullable CharSequence a2) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer insert(int a1, @Nullable CharSequence a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public StringBuffer insert(int a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer insert(int a1, char a2) { throw new RuntimeException("skeleton method"); }
  public StringBuffer insert(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public StringBuffer insert(int a1, long a2) { throw new RuntimeException("skeleton method"); }
  public StringBuffer insert(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public StringBuffer insert(int a1, double a2) { throw new RuntimeException("skeleton method"); }
  public int indexOf(String a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int indexOf(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(String a1) { throw new RuntimeException("skeleton method"); }
  public synchronized int lastIndexOf(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public synchronized StringBuffer reverse() { throw new RuntimeException("skeleton method"); }
  public synchronized String toString() { throw new RuntimeException("skeleton method"); }
}
