// Test case for Issue 142
// http://code.google.com/p/checker-framework/issues/detail?id=142
class GenericTest13 {
  interface Entry<K, V> { V getValue(); }
  interface Iterator<E> { E next(); }

  <S> S call(Iterator<? extends Entry<?, S>> entryIterator) {
    return entryIterator.next().getValue();
  }
}
