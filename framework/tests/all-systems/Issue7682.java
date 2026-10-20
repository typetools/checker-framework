public class Issue7682 {
  interface FailureRunnable<X extends Throwable> {
    void onFailure() throws X;
  }

  static <T> T doThrow() {
    throw new IllegalStateException();
  }

  <X extends Throwable> void run(FailureRunnable<X> onFailure) throws X {}

  <X extends Throwable> void run() throws X {
    run(Issue7682::doThrow);
  }
}
