package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class StringBuilder
    extends AbstractStringBuilder
    implements java.io.Serializable, CharSequence {
  private static final long serialVersionUID = 0;
  public StringBuilder() { throw new RuntimeException("skeleton method"); }
  public StringBuilder(int a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder(String a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder(CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder append(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder append(@Nullable String a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder append(@Nullable StringBuffer a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder append(@Nullable CharSequence a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder append(@Nullable CharSequence a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public StringBuilder append(char[] a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder append(char[] a1, int a2, int a3) { throw new RuntimeException("skeleton method"); }
  public StringBuilder append(boolean a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder append(char a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder append(int a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder append(long a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder append(float a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder append(double a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder appendCodePoint(int a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder delete(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public StringBuilder deleteCharAt(int a1) { throw new RuntimeException("skeleton method"); }
  public StringBuilder replace(int a1, int a2, String a3) { throw new RuntimeException("skeleton method"); }
  public StringBuilder insert(int a1, char[] a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public StringBuilder insert(int a1, @Nullable Object a2) { throw new RuntimeException("skeleton method"); }
  public StringBuilder insert(int a1, @Nullable String a2) { throw new RuntimeException("skeleton method"); }
  public StringBuilder insert(int a1, char[] a2) { throw new RuntimeException("skeleton method"); }
  public StringBuilder insert(int a1, @Nullable CharSequence a2) { throw new RuntimeException("skeleton method"); }
  public StringBuilder insert(int a1, @Nullable CharSequence a2, int a3, int a4) { throw new RuntimeException("skeleton method"); }
  public StringBuilder insert(int a1, boolean a2) { throw new RuntimeException("skeleton method"); }
  public StringBuilder insert(int a1, char a2) { throw new RuntimeException("skeleton method"); }
  public StringBuilder insert(int a1, int a2) { throw new RuntimeException("skeleton method"); }
  public StringBuilder insert(int a1, long a2) { throw new RuntimeException("skeleton method"); }
  public StringBuilder insert(int a1, float a2) { throw new RuntimeException("skeleton method"); }
  public StringBuilder insert(int a1, double a2) { throw new RuntimeException("skeleton method"); }
  public int indexOf(String a1) { throw new RuntimeException("skeleton method"); }
  public int indexOf(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(String a1) { throw new RuntimeException("skeleton method"); }
  public int lastIndexOf(String a1, int a2) { throw new RuntimeException("skeleton method"); }
  public StringBuilder reverse() { throw new RuntimeException("skeleton method"); }
  public String toString() { throw new RuntimeException("skeleton method"); }
}
