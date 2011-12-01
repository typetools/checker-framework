package java.lang;

import checkers.quals.*;

public abstract class Enum<E extends java.lang.Enum<E>> implements java.lang.Comparable<E>, java.io.Serializable {
  public final java.lang.String name() { throw new RuntimeException("skeleton method"); }
  public final int ordinal() { throw new RuntimeException("skeleton method"); }
  public java.lang.String toString() { throw new RuntimeException("skeleton method"); }
  public final boolean equals(java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public final int hashCode() { throw new RuntimeException("skeleton method"); }
  public final int compareTo(E a1) { throw new RuntimeException("skeleton method"); }
  public final java.lang.Class<E> getDeclaringClass() { throw new RuntimeException("skeleton method"); }
  public static <T extends java.lang.Enum<T>> @NonNull T valueOf(@NonNull java.lang.Class<@NonNull T> a1, @NonNull java.lang.String a2) { throw new RuntimeException("skeleton method"); }
}
