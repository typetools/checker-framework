// Test case for Issue 142
// http://code.google.com/p/checker-framework/issues/detail?id=142
class GenericTest13 {
  interface Entry<K extend Object, V extend Object> { V getValue(); }
  interface Iterator<E extend Object> { E next(); }

  <S extend Object> S call(Iterator<? extends Entry<?, S>> entryIterator) {
    return entryIterator.next().getValue();
  }
}
