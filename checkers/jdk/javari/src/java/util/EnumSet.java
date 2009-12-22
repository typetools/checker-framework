package java.util;
import checkers.javari.quals.*;

public abstract class EnumSet<E extends java.lang.Enum<E>> extends java.util.AbstractSet<E> implements java.lang.Cloneable, java.io.Serializable {
  protected EnumSet() {}
  public static <E extends java.lang.Enum<E>> java.util.EnumSet<E> noneOf(@ReadOnly java.lang.Class<E> a1) { throw new RuntimeException(("skeleton method")); }
  public static <E extends java.lang.Enum<E>> java.util.EnumSet<E> allOf(@ReadOnly java.lang.Class<E> a1) { throw new RuntimeException(("skeleton method")); }
  public static <E extends java.lang.Enum<E>> java.util.EnumSet<E> copyOf(@ReadOnly java.util.EnumSet<E> a1) { throw new RuntimeException(("skeleton method")); }
  public static <E extends java.lang.Enum<E>> java.util.EnumSet<E> copyOf(@ReadOnly java.util.Collection<E> a1) { throw new RuntimeException(("skeleton method")); }
  public static <E extends java.lang.Enum<E>> java.util.EnumSet<E> complementOf(@ReadOnly java.util.EnumSet<E> a1) { throw new RuntimeException(("skeleton method")); }
  public static <E extends java.lang.Enum<E>> java.util.EnumSet<E> of(E a1) { throw new RuntimeException(("skeleton method")); }
  public static <E extends java.lang.Enum<E>> java.util.EnumSet<E> of(E a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public static <E extends java.lang.Enum<E>> java.util.EnumSet<E> of(E a1, E a2, E a3) { throw new RuntimeException(("skeleton method")); }
  public static <E extends java.lang.Enum<E>> java.util.EnumSet<E> of(E a1, E a2, E a3, E a4) { throw new RuntimeException(("skeleton method")); }
  public static <E extends java.lang.Enum<E>> java.util.EnumSet<E> of(E a1, E a2, E a3, E a4, E a5) { throw new RuntimeException(("skeleton method")); }
  public static <E extends java.lang.Enum<E>> java.util.EnumSet<E> of(E a1, E @ReadOnly... a2) { throw new RuntimeException(("skeleton method")); }
  public static <E extends java.lang.Enum<E>> java.util.EnumSet<E> range(E a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public EnumSet<E> clone() { throw new RuntimeException("skeleton method"); }
}
