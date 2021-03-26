package inference.guava;

import java.util.EnumSet;
import java.util.Set;

@SuppressWarnings("all") // Just check for crashes.
public class Bug3 {

  public abstract static class MySet<E> implements Set<E> {

    public static <E> MySet<E> of() {
      throw new RuntimeException("");
    }

    public static <E> MySet<E> of(E e) {
      throw new RuntimeException("");
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  static MySet asMySet(EnumSet set) {
    return MySet.of(getElement(set));
  }

  public static <T> T getElement(Iterable<T> iterable) {
    throw new RuntimeException();
  }
}
