import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class GenericsCasts {
  // Cast from a raw type to a generic type
  // :: warning: [unchecked] unchecked cast
  List<Object>[] o = (List<Object>[]) new List[] {new ArrayList()};

  class Data<T> {}

  // Use our own dummy method as to avoid a complaint from the Signature Checker
  Data<?> forName(String p) {
    throw new Error("");
  }

  void m() {
    // Cast from a wildcard to a normal type argument.
    // Warning only with -AcheckCastElementType.
    // TODO:: warning: (cast.unsafe)
    // :: warning: [unchecked] unchecked cast
    Data<GenericsCasts> c = (Data<GenericsCasts>) forName("HaHa!");
  }

  // Casts from something with one type argument to two type arguments
  // are currently problematic.
  // TODO: try to find a problem with skipping this check.
  class Test<K extends Object, V extends Object> {
    class Entry<K extends Object, V extends Object> extends LinkedList<K> {}

    class Queue<T extends Object> {
      List<? extends T> poll() {
        throw new Error("");
      }
    }

    void trouble() {
      Queue<K> queue = new Queue<>();
      // Warning only with -AcheckCastElementType.
      // TODO:: warning: (cast.unsafe)
      // :: warning: [unchecked] unchecked cast
      Entry<K, V> e = (Entry<K, V>) queue.poll();
    }
  }

  public static <T extends Object> int indexOf(T[] a) {
    return indexOfEq(a);
  }

  public static int indexOfEq(Object[] a) {
    return 0;
  }
}
