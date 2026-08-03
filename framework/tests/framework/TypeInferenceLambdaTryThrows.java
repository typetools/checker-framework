// Test that inferring the checked exceptions thrown by a lambda body (JLS 18.2.5) handles a `try`
// statement.
//
// `CheckedExceptionsUtil`'s two `visitTry` methods passed the result of `scan(...)` straight to
// `List.addAll`.  `TreeScanner.scan(Iterable, P)` returns null for an empty iterable,
// `TreeScanner.scan(Tree, P)` returns null for a null tree, and both return null when the trees
// they scanned can throw no checked exception.  So a `try` whose resources, catch clauses, or
// `finally` block are absent or throw nothing used to pass null to `List.addAll`, which threw a
// NullPointerException.  Every method below used to crash type argument inference, which is
// reported as a `type.argument.inference.crashed` error.

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class TypeInferenceLambdaTryThrows {

  interface ThrowingRunnable<E extends Exception> {
    void run() throws E;
  }

  // The `throws E` mentions the inference variable, so a `→ throws` constraint is created and the
  // lambda body is scanned for the checked exceptions it can throw.
  static <E extends Exception> void run(ThrowingRunnable<E> r) throws E {}

  // No resources and no `finally` block.
  static void tryCatch() {
    run(
        () -> {
          try {
            throw new IOException();
          } catch (IOException e) {
            // Do nothing.
          }
        });
  }

  // No resources and no catch clauses.
  static void tryFinally() throws IOException {
    run(
        () -> {
          try {
            throw new IOException();
          } finally {
            System.out.println();
          }
        });
  }

  // No resources.
  static void tryCatchFinally() {
    run(
        () -> {
          try {
            throw new IOException();
          } catch (IOException e) {
            // Do nothing.
          } finally {
            System.out.println();
          }
        });
  }

  // Resources, catch clauses, and a `finally` block are all present, but the `finally` block can
  // throw nothing, so scanning it still returns null.
  static void tryWithResources() {
    run(
        () -> {
          try (Reader r = new StringReader("")) {
            r.read();
          } catch (IOException e) {
            // Do nothing.
          } finally {
            System.out.println();
          }
        });
  }

  // A lambda body with a `try` from which a checked exception escapes: E is inferred as
  // IOException, so the caller must declare it.
  static void checkedExceptionEscapesTry() throws IOException {
    run(
        () -> {
          try {
            throw new IOException();
          } catch (RuntimeException e) {
            // Do nothing.
          }
        });
  }
}
