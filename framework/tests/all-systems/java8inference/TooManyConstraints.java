import java.lang.ref.ReferenceQueue;

@SuppressWarnings("all") // Just check for crashes.
public class TooManyConstraints {

  static final class ThisClass<K, V> {
    private final ReferenceQueue<V> queue = new ReferenceQueue<V>();

    public InterfaceB<K, V, EntryC<K, V>> test(Entry<K, V, ?> e, V value) {
      return new ClassA<>(queue, value, cast(e));
    }

    public EntryC<K, V> cast(Entry<K, V, ?> entry) {
      throw new RuntimeException();
    }
  }

  static final class ClassA<K, V, E extends Entry<K, V, E>> implements InterfaceB<K, V, E> {
    ClassA(ReferenceQueue<V> queue, V referent, E entry) {}
  }

  interface InterfaceB<K, V, E extends Entry<K, V, E>> {}

  static final class EntryC<K, V> implements EntryB<K, V, EntryC<K, V>> {}

  interface EntryB<K, V, E extends Entry<K, V, E>> extends Entry<K, V, E> {}

  interface Entry<K, V, E extends Entry<K, V, E>> {}
}
