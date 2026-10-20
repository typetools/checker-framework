public class Issue7693 {
  interface CheckedRunnable<X extends Throwable> {
    void run() throws X;
  }

  <X extends Throwable> void call(CheckedRunnable<X> runnable) throws X {}

  void test() throws Exception {
    call(
        () -> {
          try {
            System.out.println();
          } catch (RuntimeException e) {
          }
        });
  }
}
