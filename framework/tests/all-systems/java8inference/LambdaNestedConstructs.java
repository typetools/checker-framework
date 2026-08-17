// Test case for the JLS 11.2.2 checked-exception constraints that are generated for a lambda whose
// body contains a nested lambda, an anonymous class, or a local class.  An exception thrown in the
// body of a nested lambda, or in a method of a nested class, is attributed to that construct rather
// than to the enclosing lambda.
//
// In the methods that call `runUnchecked`, the inference variable's declared upper bound is
// RuntimeException.  Attributing a checked exception thrown by a nested construct to the lambda
// gives the inference variable the lower bound IOException, which is inconsistent with that upper
// bound; that is reported as a "type.argument.inference.crashed" error.
//
// The methods that call `runChecked` show the exceptions that are attributed to the lambda: the
// inference variable's upper bound is Exception, and inference succeeds with IOException.

import java.io.IOException;

public class LambdaNestedConstructs {

  @FunctionalInterface
  interface UncheckedRunnable<E extends RuntimeException> {
    void run() throws E;
  }

  @FunctionalInterface
  interface ThrowingRunnable<E extends Exception> {
    void run() throws E;
  }

  @FunctionalInterface
  interface IoOperation {
    void run() throws IOException;
  }

  /**
   * The {@code throws E} on {@code UncheckedRunnable.run()} makes inference generate a
   * checked-exception constraint for a lambda.
   */
  static <E extends RuntimeException> void runUnchecked(UncheckedRunnable<E> r) {}

  /** The {@code throws E} makes inference generate a checked-exception constraint for a lambda. */
  static <E extends Exception> void runChecked(ThrowingRunnable<E> r) throws E {}

  static void readIt() throws IOException {
    throw new IOException();
  }

  /**
   * Returns a string.
   *
   * @return a string
   * @throws IOException always
   */
  static String readItAndReturn() throws IOException {
    throw new IOException();
  }

  /** The nested lambda's body throws IOException, but the outer lambda's body does not. */
  static void nestedLambda() {
    runUnchecked(
        () -> {
          IoOperation nested = () -> readIt();
        });
  }

  /** The nested lambda's body throws IOException by a class instance creation expression. */
  static void nestedLambdaNewClass() {
    runUnchecked(
        () -> {
          IoOperation nested = () -> new StringBuilder(readItAndReturn());
        });
  }

  /** A method of the anonymous class throws IOException, but the outer lambda's body does not. */
  static void anonymousClassMethod() {
    runUnchecked(
        () -> {
          IoOperation nested =
              new IoOperation() {
                @Override
                public void run() throws IOException {
                  readIt();
                }
              };
        });
  }

  /** A method of the local class throws IOException, but the outer lambda's body does not. */
  static void localClassMethod() {
    runUnchecked(
        () -> {
          class Local {
            void m() throws IOException {
              readIt();
            }
          }
        });
  }

  /**
   * An argument to a class instance creation expression in the lambda's body throws IOException.
   */
  static void newClassArgument() throws IOException {
    runChecked(
        () -> {
          Object o = new StringBuilder(readItAndReturn());
        });
  }

  /** A method invocation in the lambda's body throws IOException. */
  static void methodInvocation() throws IOException {
    runChecked(
        () -> {
          readIt();
        });
  }
}
