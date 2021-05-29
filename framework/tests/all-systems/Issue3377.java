// @below-java11-jdk-skip-test
@SuppressWarnings("all") // Check for crashes.
public class Issue3377 {
  static class Box<S> {}

  interface Unboxer {
    <T> T unbox(Box<T> p);
  }

  static class Crash {
    Box<String> crash(Unboxer ub) {
      return ub.unbox(new Box<>() {});
    }
  }
}
