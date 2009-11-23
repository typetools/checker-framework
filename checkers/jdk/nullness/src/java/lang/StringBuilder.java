package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class StringBuilder
    extends AbstractStringBuilder
    implements java.io.Serializable, CharSequence {
  private static final long serialVersionUID = 0;
  public StringBuilder() { throw new RuntimeException("skeleton method"); }
  public StringBuilder(int a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder(java.lang.CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder append(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder append(@Nullable java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder append(@Nullable java.lang.StringBuffer a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder append(@Nullable java.lang.CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder append(@Nullable java.lang.CharSequence a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder append(char[] a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder append(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder append(boolean a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder append(char a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder append(int a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder append(long a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder append(float a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder append(double a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder appendCodePoint(int a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder delete(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder deleteCharAt(int a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder replace(int a1, int a2, java.lang.String a3) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder insert(int a1, char[] a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder insert(int a1, @Nullable java.lang.Object a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder insert(int a1, @Nullable java.lang.String a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder insert(int a1, char[] a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder insert(int a1, @Nullable java.lang.CharSequence a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder insert(int a1, @Nullable java.lang.CharSequence a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder insert(int a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder insert(int a1, char a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder insert(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder insert(int a1, long a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder insert(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder insert(int a1, double a2) { throw new RuntimeException("skeleton method"); }
  public int indexOf(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(java.lang.String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(java.lang.String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public java.lang.StringBuilder reverse() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
}
