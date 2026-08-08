package open.falsepos;

import org.checkerframework.checker.tainting.qual.Untainted;

public class Issue7723 {
  record Box<T>(T value) {}

  void test(@Untainted Object foo2) {
    Object foo = foo2;
    var boxed = new Box<>(foo);
  }
}
