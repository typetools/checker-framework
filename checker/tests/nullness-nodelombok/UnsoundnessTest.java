// An example of an unsoundness that occurs when running the Nullness Checker
// on Lombok'd code without running delombok first.

@lombok.Builder
class UnsoundnessTest {
  @lombok.NonNull Object foo;
  @lombok.NonNull Object bar;

  static void test() {
    // :: error: (assignment)
    builder().foo(null).build();
  }
}
