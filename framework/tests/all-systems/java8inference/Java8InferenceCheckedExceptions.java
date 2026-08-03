// The exception types that JLS 18.2.5 collects for a lambda body are its *checked* exceptions: the
// ones that are neither a subclass of RuntimeException nor a subclass of Error (JLS 11.1.1).
// `CheckedExceptionsUtil.isCheckedException` used to test the opposite, so `‹Xi <: Ej›` constraints
// were generated for the unchecked exceptions and not for the checked ones.
//
// In `uncheckedOnly` below, the lambda throws only an unchecked exception, so JLS 18.2.5 generates
// no constraint at all and E is resolved to IOException.  With the predicate inverted, the
// constraint `‹IllegalStateException <: E›` was generated instead; together with the declared bound
// `E extends IOException` that is unsatisfiable, and inference failed.

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
