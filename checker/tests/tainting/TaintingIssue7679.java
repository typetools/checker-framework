import org.checkerframework.checker.tainting.qual.Untainted;

public class TaintingIssue7679 {
  static class FooException extends RuntimeException {}

  static class BarException extends RuntimeException {}

  static class Box<T> {
    Box(T t) {}
  }

  static <T> void handle(Box<@Untainted T> box) {}

  static <T> void handle2(Box<T> box) {}

  void test() throws Exception {
    try {
      x();
    } catch (FooException | BarException e) {
      // :: error: [argument]
      handle(new Box<>(e));
      handle2(new Box<>(e));
    }
  }

  void x() {}
}
