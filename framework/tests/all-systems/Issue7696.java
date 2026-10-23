public class Issue7696 {
  interface Box<T> {}

  interface Matcher<T> {}

  static class MyMatcher<T> implements Matcher<Box<T>> {}

  static <T> T argThat(Matcher<? super T> matcher) {
    throw new UnsupportedOperationException();
  }

  static <E> Box<E> matches() {
    return argThat(new MyMatcher<>());
  }
}
