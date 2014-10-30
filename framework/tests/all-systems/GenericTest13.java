// Test case for Issue 142
// http://code.google.com/p/checker-framework/issues/detail?id=142
class GenericTest13 {
  interface Entry<K extends Object, V extends Object> { V getValue(); }
  interface Iterator<E extends Object> { E next(); }

  <S extends Object> S call(Iterator<? extends Entry<?, S>> entryIterator) {
    return entryIterator.next().getValue();
  }
}
