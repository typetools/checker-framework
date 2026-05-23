// https://github.com/typetools/checker-framework/issues/7681
public class Issue7681 {

  /**
   * Transforms a Generic of I to a Generic of O using the given function.
   *
   * @param <I> the input type
   * @param <O> the output type
   * @param input the input
   * @param function the function to apply
   * @return the transformed value
   */
  static <I, O> Generic<O> transform(Generic<I> input, Function<? super I, ? extends O> function) {
    throw new UnsupportedOperationException();
  }

  /**
   * A functional interface with one type parameter for input and one for output.
   *
   * @param <F> the input type
   * @param <T> the output type
   */
  interface Function<F, T> {
    /**
     * Applies this function to the given argument.
     *
     * @param f the input
     * @return the output
     */
    T apply(F f);
  }

  /**
   * A generic container type.
   *
   * @param <T> the type of the contained value
   */
  interface Generic<T> {}

  /** Converts Generic values. */
  static class GenericConverter {
    /**
     * Returns the input unchanged.
     *
     * @param <T> the type
     * @param input the input
     * @return the input unchanged
     */
    static <T> Generic<T> passthru(Generic<T> input) {
      throw new UnsupportedOperationException();
    }
  }

  /** A foo with a double-generic method. */
  interface Foo {
    /**
     * Returns a doubly-nested Generic.
     *
     * @return a doubly-nested Generic
     */
    Generic<Generic<Object>> doubleGeneric();
  }

  /**
   * Test case for nested generics with lambda and method reference.
   *
   * @param foos input
   * @param defaultVal default value
   * @param b condition
   */
  void test(Generic<Foo> foos, Generic<Generic<Object>> defaultVal, boolean b) {
    Generic<Generic<Generic<Object>>> result =
        transform(
            foos,
            foo -> b ? transform(foo.doubleGeneric(), GenericConverter::passthru) : defaultVal);
  }
}
