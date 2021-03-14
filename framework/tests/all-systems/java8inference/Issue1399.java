// Test case for Issue 1399.
// https://github.com/typetools/checker-framework/issues/1399

public class Issue1399 {
  static class Box<T> {
    static <T> Box<T> box(Class<T> type) {
      return new Box<T>();
    }

    void act(T instance) {}
  }

  abstract class Math {
    abstract <T> Box<T> id(Box<T> in);

    void foo(Math m) {
      m.id(Box.box(Long.class)).act(10L);
    }
  }
}
