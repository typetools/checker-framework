// Test case for Issue 1411:
// https://github.com/typetools/checker-framework/issues/1411

import org.checkerframework.dataflow.qual.Pure;

interface IGeneric<V> {
  @Pure
  public V get();
}

interface IConcrete extends IGeneric<char[]> {}

public class Issue1411 {
  static void m(IConcrete ic) {
    char[] val = ic.get();
  }
}
