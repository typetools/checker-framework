// Test case for Issue 367:
// https://code.google.com/p/checker-framework/issues/detail?id=367
// @skip-test This requires capture to typecheck correctly

class Test {
    static void test(Iterable<? extends Thread> threads) {
        threads.forEach(thread -> System.out.println(thread));
    }
}