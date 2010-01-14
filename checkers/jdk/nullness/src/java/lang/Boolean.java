package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public final class Boolean implements java.io.Serializable, java.lang.Comparable<java.lang.Boolean> {
  private static final long serialVersionUID = 0;
  public final static java.lang.Boolean TRUE;
  public final static java.lang.Boolean FALSE;
  public final static java.lang.Class<java.lang.Boolean> TYPE;
  public Boolean(boolean a1) { throw new RuntimeException("skeleton method"); }
  public Boolean(@Nullable java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public static boolean parseBoolean(@Nullable java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public boolean booleanValue() { throw new RuntimeException("skeleton method"); }
  public static java.lang.Boolean valueOf(boolean a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.Boolean valueOf(@Nullable java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public static java.lang.String toString(boolean a1) { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public int hashCode() { throw new RuntimeException("skeleton method"); }
  public boolean equals(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public static boolean getBoolean(@Nullable java.lang.String a1) { throw new RuntimeException("skeleton method"); }
  public int compareTo(java.lang.Boolean a1) { throw new RuntimeException("skeleton method"); }
}
