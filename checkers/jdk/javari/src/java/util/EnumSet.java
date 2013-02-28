package java.util;
import checkers.javari.quals.*;

public abstract class EnumSet<E extends Enum<E>> extends AbstractSet<E> implements Cloneable, java.io.Serializable {
  protected EnumSet() {}
  public static <E extends Enum<E>> EnumSet<E> noneOf(@ReadOnly Class<E> a1) { throw new RuntimeException(("skeleton method")); }
  public static <E extends Enum<E>> EnumSet<E> allOf(@ReadOnly Class<E> a1) { throw new RuntimeException(("skeleton method")); }
  public static <E extends Enum<E>> EnumSet<E> copyOf(@ReadOnly EnumSet<E> a1) { throw new RuntimeException(("skeleton method")); }
  public static <E extends Enum<E>> EnumSet<E> copyOf(@ReadOnly Collection<E> a1) { throw new RuntimeException(("skeleton method")); }
  public static <E extends Enum<E>> EnumSet<E> complementOf(@ReadOnly EnumSet<E> a1) { throw new RuntimeException(("skeleton method")); }
  public static <E extends Enum<E>> EnumSet<E> of(E a1) { throw new RuntimeException(("skeleton method")); }
  public static <E extends Enum<E>> EnumSet<E> of(E a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public static <E extends Enum<E>> EnumSet<E> of(E a1, E a2, E a3) { throw new RuntimeException(("skeleton method")); }
  public static <E extends Enum<E>> EnumSet<E> of(E a1, E a2, E a3, E a4) { throw new RuntimeException(("skeleton method")); }
  public static <E extends Enum<E>> EnumSet<E> of(E a1, E a2, E a3, E a4, E a5) { throw new RuntimeException(("skeleton method")); }
  @SuppressWarnings({"varargs","unchecked"})
  public static <E extends Enum<E>> EnumSet<E> of(E a1, E @ReadOnly... a2) { throw new RuntimeException(("skeleton method")); }
  public static <E extends Enum<E>> EnumSet<E> range(E a1, E a2) { throw new RuntimeException(("skeleton method")); }
  public EnumSet<E> clone() { throw new RuntimeException("skeleton method"); }
}
