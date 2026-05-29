@SuppressWarnings("all")
public class Issue7700 {
  @SuppressWarnings("unchecked")
  interface Builder<K, V> {
    Builder<K, V> putAll(K key, V... values);
  }

  interface BiAccumulator<A, K, V> {
    void accept(A a, K k, V v);
  }

  interface BiStream<K, V> {
    <A> A collect(A container, BiAccumulator<? super A, ? super K, ? super V> accumulator);
  }

  static <K, V> Builder<K, V> builder() {
    throw new UnsupportedOperationException();
  }

  void test(BiStream<String, String> stream) {
    stream.collect(builder(), Builder::putAll);
  }
}
