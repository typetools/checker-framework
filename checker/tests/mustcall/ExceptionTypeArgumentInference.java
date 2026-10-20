// Earlier versions of the Checker Framework would report cryptic type inference errors on this
// code.

import java.io.IOException;
import org.checkerframework.checker.mustcall.qual.*;

public class ExceptionTypeArgumentInference {

  interface ThrowingRunnable<E extends Throwable> {
    void run() throws E;
  }

  <E extends Throwable> void run(ThrowingRunnable<E> job) throws E {
    job.run();
  }

  void testRunNoThrow() {
    this.run(() -> {});
  }

  void testRunThrowIOException() throws IOException {
    this.run(
        () -> {
          throw new IOException();
        });
  }

  void testRunNoThrowMethodReference() {
    this.run(this::testRunNoThrow);
  }

  void testRunThrowIOExceptionMethodReference() throws IOException {
    this.run(this::testRunThrowIOException);
  }
}
