import java.util.function.Supplier;

class Issue6652 {
  interface Arbitrary<T> {}

  Arbitrary<Supplier<String>> test() {
    return lazyOf(() -> of(() -> "foo"));
  }

  static <T> Arbitrary<T> of(T value) {
    throw new UnsupportedOperationException("implementation omitted");
  }

  static <T> Arbitrary<T> lazyOf(Supplier<Arbitrary<? extends T>> supplier) {
    throw new UnsupportedOperationException("implementation omitted");
  }
}
