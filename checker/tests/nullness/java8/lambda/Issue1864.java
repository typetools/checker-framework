// Test case for Issue 1864:
// https://github.com/typetools/checker-framework/issues/1864

import java.util.List;
import java.util.function.Supplier;

abstract class Issue1864 {
  interface A {}

  abstract <T extends A> List<T> g();

  abstract void h(Supplier<Iterable<A>> s);

  void f() {
    Iterable<A> xs = g();
    h(() -> xs);
  }
}
