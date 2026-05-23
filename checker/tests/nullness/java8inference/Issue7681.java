// https://github.com/typetools/checker-framework/issues/7681
public class Issue7681 {

  static <I, O> Generic<O> transform(Generic<I> input, Function<? super I, ? extends O> function) {
    throw new UnsupportedOperationException();
  }

  interface Function<F, T> {
    T apply(F f);
  }

  interface Generic<T> {}

  static class GenericConverter {
    static <T> Generic<T> passthru(Generic<T> input) {
      throw new UnsupportedOperationException();
    }
  }

  interface Foo {
    Generic<Generic<Object>> doubleGeneric();
  }

  void test(Generic<Foo> foos, Generic<Generic<Object>> defaultVal, boolean b) {
    Generic<Generic<Generic<Object>>> result =
        transform(
            foos,
            foo -> b ? transform(foo.doubleGeneric(), GenericConverter::passthru) : defaultVal);
  }
}
