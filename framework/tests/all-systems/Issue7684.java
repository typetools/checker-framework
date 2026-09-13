// Test case for Issue 7684:
// https://github.com/typetools/checker-framework/issues/7684

import java.io.Serializable;

public class Issue7684<T> {
  interface Consumer<T> {
    void accept(T t);
  }

  static class SerializedOp<T> {
    SerializedOp(Consumer<T> consumer) {}
  }

  static class Foo {
    <T> void bar(T t) {}
  }

  void test(Foo foo) {
    new SerializedOp<T>((Consumer<T> & Serializable) foo::bar);
  }
}
