// Test case for https://github.com/typetools/checker-framework/issues/8238

public class Issue8238 {

  interface Fn<A, B> {
    B apply(A a);
  }

  static class Tag<T> {}

  static class Box<K, V> {
    V compute(K key, Fn<? super K, ? extends V> fn) {
      throw new UnsupportedOperationException();
    }
  }

  Box<Tag<?>, Object> box = new Box<>();

  <T> Object create(Tag<T> tag) {
    throw new UnsupportedOperationException();
  }

  <T> void get(Tag<T> tag) {
    box.compute(tag, this::create);
  }

  static class Outer<A> {
    class Inner<B> {}
  }

  <A, B> void takeInner(Outer<A>.Inner<B> inner) {}

  // The wildcard that must be captured is on the enclosing type, which is reached only through the
  // constraint between the enclosing types of Outer<?>.Inner<?> and Outer<A>.Inner<B>.
  void enclosingWildcard(Outer<?>.Inner<?> inner) {
    takeInner(inner);
  }
}
