import java.util.*;

// Clone of oigj/GenericsCasts
class GenericsCasts {
  // Cast from a raw type to a generic type
  List<Object>[] o = (List<Object>[]) new List[5];

  // Use our own dummy method as to avoid a complaint from the Signature Checker
  Class<?> forName(String p) { throw new Error(""); }
  void m() {
      // This warning unfortunately doesn't show up for all type systems.
      // Otherwise, this test case should be applicable to other systems.
      // Cast from a wildcard to a normal type argument.
      Class<GenericsCasts> c = (Class<GenericsCasts>) forName("HaHa!");
  }

  // Casts from something with one type argument to two type arguments
  // are currently problematic.
  // TODO: try to find a problem with skipping this check.
  class Test<K, V> {
      class Entry<K, V> extends LinkedList<K> {}
      class Queue<T> {
          List<? extends T> poll() { throw new Error(""); }
      }
      void trouble() {
          Queue<K> queue = new Queue<K>();
          Entry<K, V> e = (Entry<K, V>) queue.poll();
      }
  }
  
  public static <T> int indexOf(T[] a) {
      return indexOfEq(a);
  }
  public static int indexOfEq(Object[] a) {
      return 0;
  }

}  
