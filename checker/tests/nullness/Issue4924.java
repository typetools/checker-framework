// Test case for issue #4924: https://tinyurl.com/cfissue/4924

// @skip-test until the issue is fixed

class Issue4924 {}

interface Callback4924<T> {}

class Template4924<T> {
    interface Putter4924<T> {
        void put(T result);
    }

    class Adapter4924 implements Callback4924<T> {
        Adapter4924(Putter4924<T> putter) {}
    }
}

class Super4924<T> extends Template4924<T> {}

class Issue extends Super4924<String> {
    void go(Callback4924<String> callback) {}

    void foo() {
        go(new Adapter4924(result -> {}));
    }
}
