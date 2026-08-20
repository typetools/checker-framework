// Caught crash.
public class Issue7681 {
  static <I, O> Generic<O> transform(Generic<I> input, Function<? super I, ? extends O> function) {
    throw new UnsupportedOperationException();
  }

  static <I2, O2> Generic<O2> transform2(
      Generic<I2> input, Function<? super I2, ? extends O2> function) {
    throw new UnsupportedOperationException();
  }

  interface Function<F, T> {
    T apply(F f);
  }

  interface Generic<G> {}

  static class GenericConverter {
    static <Z> Generic<Z> passthru(Generic<Z> optional) {
      throw new UnsupportedOperationException();
    }
  }

  interface Foo {
    Generic<Generic<Object>> doubleGeneric();
  }

  void test(Generic<Foo> foos, Generic<Generic<Object>> defaultVal, boolean b) {
    Generic<Generic<Generic<Object>>> calendarEventDataListFuture =
        transform(foos, foo -> transform2(foo.doubleGeneric(), GenericConverter::passthru));
  }
}
