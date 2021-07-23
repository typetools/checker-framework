// An example of an unsoundness that occurs when running the Called Methods Checker
// on Lombok'd code without running delombok first.

@lombok.Builder
class UnsoundnessTest {
  @lombok.NonNull Object foo;
  @lombok.NonNull Object bar;

  static void test() {
    // :: error: (finalizer.invocation)
    builder().build();
  }

  static void test2() {
    builder().foo(null).bar(null).build();
  }
}
