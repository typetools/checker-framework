// Test case for Issue 2779
// https://github.com/typetools/checker-framework/issues/2779

// @below-java9-jdk-skip-test
@SuppressWarnings("all") // Just check for crashes.
interface Issue2779<S> {
  S get();

  static <T> Issue2779<T> wrap2(T val) {
    return new Issue2779<T>() {
      @Override
      public T get() {
        return val;
      }
    };
  }

  static <T> Issue2779<T> wrap(T val) {
    return new Issue2779<>() {
      @Override
      public T get() {
        return val;
      }
    };
  }
}
