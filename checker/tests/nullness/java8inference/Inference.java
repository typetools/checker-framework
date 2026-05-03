// Test case for Issue 979:
// https://github.com/typetools/checker-framework/issues/979

class MyStream<T> {
  @SuppressWarnings("nullness")
  <R, A> R collect(MyCollector<? super T, A, R> collector) {
    return null;
  }
}

interface MyCollector<T, A, R> {}

public class Inference {

  @SuppressWarnings("nullness")
  static <E> MyCollector<E, ?, MyStream<E>> toImmutableStream() {
    return null;
  }

  MyStream<String> test(MyStream<String> p) {
    return p.collect(toImmutableStream());
  }
}
