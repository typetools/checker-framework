// Test case for Issue 1379.
// https://github.com/typetools/checker-framework/issues/1379

interface Box1379<V> {}

interface Trans1379<I, O> {
  Box1379<O> apply(I in);
}

@SuppressWarnings("all") // just check for crashes
abstract class Issue1379 {
  abstract <I, O> Box1379<O> app(Box1379<I> in, Trans1379<? super I, ? extends O> t);

  abstract <I, O> Trans1379<I, O> pass(Trans1379<I, O> t);

  abstract Box1379<Number> box(Number p);

  void foo(Box1379<Number> p) {
    app(p, pass(this::box));
  }
}
