// Test case for Issue 367:
// https://github.com/typetools/checker-framework/issues/367
// @skip-test This requires capture to typecheck correctly

class Test {
    static void test(Iterable<? extends Thread> threads) {
        threads.forEach(thread -> System.out.println(thread));
    }
}
