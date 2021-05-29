// Test case for Issue 363:
// https://github.com/typetools/checker-framework/issues/363

public class Issue363 {
  void foo(java.util.OptionalInt value) {
    value.orElseThrow(() -> new Error());
  }

  void bar(java.util.OptionalInt value) {
    java.util.function.Supplier<Error> s = () -> new Error();
    value.orElseThrow(s);
  }
}
