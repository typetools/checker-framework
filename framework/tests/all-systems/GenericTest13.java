// Test case for Issue 142
// https://github.com/typetools/checker-framework/issues/142

public class GenericTest13 {
  interface Entry<K extends Object, V extends Object> {
    V getValue();
  }

  interface Iterator<E extends Object> {
    E next();
  }

  <S extends Object> S call(Iterator<? extends Entry<?, S>> entryIterator) {
    return entryIterator.next().getValue();
  }
}
