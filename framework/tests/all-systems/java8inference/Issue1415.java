// Test case for Issue 1415.
// https://github.com/typetools/checker-framework/issues/1415

@SuppressWarnings("all") // Check for crashes.
public class Issue1415 {
  static class Optional<T> {
    static <T> Optional<T> absent() {
      return null;
    }

    static <T> Optional<T> of(T p) {
      return null;
    }
  }

  static class Box<T> {
    void box(T p) {}
  }

  static class Crash9 {
    <F extends Enum<F>> void foo(boolean b, Box<Optional<F>> box, Class<F> enumClass) {
      box.box(b ? Optional.<F>absent() : Optional.of(Enum.valueOf(enumClass, "hi")));
      box.box(b ? Optional.absent() : Optional.of(Enum.valueOf(enumClass, "hi")));
    }
  }
}
