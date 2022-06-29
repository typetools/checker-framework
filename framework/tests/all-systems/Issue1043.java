// Test case for Issue 1043:
// https://github.com/typetools/checker-framework/issues/1043

public class Issue1043 {
  <T> boolean foo(Class<T> p) {
    return true;
  }

  void bar(Object p) {}

  @SuppressWarnings("keyfor:type.argument")
  void baz() {
    bar(foo(this.getClass()) ? "a" : "b");
  }
}
