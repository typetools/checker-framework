// Test case for Issue 1424.
// https://github.com/typetools/checker-framework/issues/1424
// @below-java8-jdk-skip-test

@SuppressWarnings("unchecked")
abstract class Issue1424 {
    class Box<T> {}

    interface Callable<V> {
        V call() throws Exception;
    }

    class MyCallable<T> implements Callable<T> {
        MyCallable(Callable<T> delegate) {}

        public T call() throws Exception {
            throw new RuntimeException();
        }
    }

    abstract <T> Box<T> submit(Callable<T> t);

    Box<Boolean> foo() {
        return submit(new MyCallable(() -> true));
    }
}
