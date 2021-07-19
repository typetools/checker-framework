// Test case for Issue 367:
// https://github.com/typetools/checker-framework/issues/367
public class Issue367 {
  static void test(Iterable<? extends Thread> threads) {
    threads.forEach(thread -> System.out.println(thread));
  }
}
