public class Issue7679 {
  static class FooException extends RuntimeException {}

  static class BarException extends RuntimeException {}

  static class Box<T> {
    Box(T t) {}
  }

  static <T> void handle(Box<T> box) {}

  @SuppressWarnings("confidential:type.arguments.not.inferred") // CF bug?
  void test() throws Exception {
    try {
      x();
    } catch (FooException | BarException e) {
      handle(new Box<>(e));
    }
  }

  void x() {}
}
