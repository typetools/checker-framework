// Test case for Issue 2779
// https://github.com/typetools/checker-framework/issues/2779

// @below-java9-jdk-skip-test

interface Issue2779<S> {
    S get();

    static <T> Issue2779<T> wrap(T val) {
        return new Issue2779<>() {
            @Override
            public T get() {
                return val;
            }
        };
    }
}
