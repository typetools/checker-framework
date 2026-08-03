// A `try` statement in a lambda body, where the target functional interface has an inference
// variable in its `throws` clause.  Computing the checked exceptions thrown by the lambda (JLS
// 18.2.5) scans the `try` statement.  `TreeScanner.scan(Iterable, P)` returns null for an empty
// iterable and `TreeScanner.scan(Tree, P)` returns null for a null tree, so a `try` with no
// resources or no `finally` block used to pass null to `List.addAll`, which threw a
// NullPointerException that surfaced as a `type.argument.inference.crashed` error.

import java.io.IOException;

public class Java8InferenceTryInLambda {

  static int count = 0;

  interface ThrowingSupplier<T, E extends Exception> {
    T get() throws E;
  }

  static <T, E extends Exception> T call(ThrowingSupplier<T, E> supplier) throws E {
    return supplier.get();
  }

  static String readIt() throws IOException {
    throw new IOException();
  }

  // A `try` with neither resources nor a `finally` block.
  static String noResourcesNoFinally() throws IOException {
    return call(
        () -> {
          try {
            return readIt();
          } catch (RuntimeException e) {
            return "";
          }
        });
  }

  // A `try` with a `finally` block but no resources.
  static String noResources() throws IOException {
    return call(
        () -> {
          try {
            return readIt();
          } finally {
            count = count + 1;
          }
        });
  }

  // A `try` with resources but no `finally` block.
  static String noFinally() throws IOException {
    return call(
        () -> {
          try (AutoCloseable c = () -> {}) {
            return readIt();
          } catch (Exception e) {
            return "";
          }
        });
  }
}
