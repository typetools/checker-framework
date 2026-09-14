// Test case for the JLS 18.2.5 checked-exception constraints that are generated for a lambda whose
// body contains a `try` statement.
//
// `TreeScanner.scan(Iterable, P)` returns null for an empty iterable, and
// `TreeScanner.scan(Tree, P)` returns null for a null tree.  So, when
// `CheckedExceptionsUtil.CheckedExceptionVisitor.visitTry` passed those results directly to
// `List.addAll`, every `try` with no resources, with no catch clauses, or with no `finally` block
// made type argument inference crash with a `NullPointerException`, which was reported as a
// "type.argument.inference.crashed" error.
//
// The methods below cover every legal combination of present and absent resources, catch clauses,
// and `finally` block, and both outcomes of the `removeAssignable` call that discards the
// exceptions that the catch clauses would catch.

import java.io.Closeable;
import java.io.IOException;

public class LambdaTryCheckedExceptions {

  @FunctionalInterface
  interface ThrowingSupplier<T, E extends Exception> {
    T get() throws E;
  }

  /** The `throws E` makes inference generate a checked-exception constraint for a lambda. */
  static <T, E extends Exception> T call(ThrowingSupplier<T, E> supplier) throws E {
    return supplier.get();
  }

  static String mightThrow() throws IOException {
    return "";
  }

  static String risky() {
    return "";
  }

  static Closeable open() throws IOException {
    throw new IOException();
  }

  static void cleanup() {}

  /** A `try` with a catch clause, but with no resources and no `finally` block. */
  static String tryCatch() {
    return call(
        () -> {
          try {
            return mightThrow();
          } catch (IOException e) {
            return "";
          }
        });
  }

  /** A `try` with a `finally` block, but with no resources and no catch clauses. */
  static String tryFinally() throws IOException {
    return call(
        () -> {
          try {
            return mightThrow();
          } finally {
            cleanup();
          }
        });
  }

  /**
   * A `try` whose catch clause catches nothing that the `try` block throws, so `removeAssignable`
   * runs on a non-empty list but removes nothing and the checked exception survives.
   */
  static String tryCatchUnrelated() throws IOException {
    return call(
        () -> {
          try {
            return mightThrow();
          } catch (RuntimeException e) {
            return "";
          }
        });
  }

  /** A try-with-resources with no catch clauses and no `finally` block. */
  static String tryWithResources() throws IOException {
    return call(
        () -> {
          try (Closeable c = open()) {
            return risky();
          }
        });
  }

  /** A `try` with resources and a catch clause, but no `finally` block. */
  static String tryResourcesAndCatch() throws IOException {
    return call(
        () -> {
          try (Closeable c = open()) {
            return mightThrow();
          } catch (RuntimeException e) {
            return "";
          }
        });
  }

  /** A `try` with resources, a catch clause, and a `finally` block: no scan result is null. */
  static String tryEverything() {
    return call(
        () -> {
          try (Closeable c = open()) {
            return mightThrow();
          } catch (IOException e) {
            return "";
          } finally {
            cleanup();
          }
        });
  }

  /** A bare `try` with a catch clause; nothing checked is thrown at all. */
  static String tryNothingThrown() {
    return call(
        () -> {
          try {
            return risky();
          } catch (RuntimeException e) {
            return "";
          }
        });
  }

  /**
   * The checked exception is thrown by the catch clause rather than by the `try` block, so it is
   * found only by `scan(node.getCatches(), aVoid)`.
   */
  static String throwFromCatch() throws IOException {
    return call(
        () -> {
          try {
            return risky();
          } catch (RuntimeException e) {
            return mightThrow();
          }
        });
  }

  /**
   * The checked exception is thrown by the `finally` block, so it is found only by
   * `scan(node.getFinallyBlock(), aVoid)`.
   */
  static String throwFromFinally() throws IOException {
    return call(
        () -> {
          try {
            return risky();
          } finally {
            mightThrow();
          }
        });
  }

  /** A `try` nested inside a lambda that is nested inside another lambda. */
  static String nestedLambda() throws IOException {
    return call(
        () ->
            call(
                () -> {
                  try {
                    return mightThrow();
                  } finally {
                    cleanup();
                  }
                }));
  }
}
