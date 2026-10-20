import java.io.IOException;

public class Java8InferenceCheckedExceptions {

  interface ThrowingSupplier<T, E extends IOException> {
    T get() throws E;
  }

  static <T, E extends IOException> T call(ThrowingSupplier<T, E> supplier) throws E {
    return supplier.get();
  }

  static String readIt() throws IOException {
    throw new IOException();
  }

  // The lambda throws only an unchecked exception, which contributes no constraint on E.
  static String uncheckedOnly() throws IOException {
    return call(
        () -> {
          throw new IllegalStateException();
        });
  }

  // The lambda throws only an Error, which likewise contributes no constraint on E.
  static String errorOnly() throws IOException {
    return call(
        () -> {
          throw new AssertionError();
        });
  }

  // The lambda throws a checked exception, which does contribute the constraint
  // `‹IOException <: E›`.
  static String checkedException() throws IOException {
    return call(() -> readIt());
  }
}
