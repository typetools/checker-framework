// An example of an unsoundness that occurs when running the Nullness Checker
// on Lombok'd code without running delombok first.

@lombok.Builder
class UnsoundnessTest {
  @lombok.NonNull Object foo;
  @lombok.NonNull Object bar;

  static void test() {
    // An error should be issued here, but the code has not been delombok'd.
    // If the CF and Lombok are ever able to work in the same invocation of javac
    // (i.e. without delomboking first), then this error should be changed back to an
    // expected error by re-adding the leading "::".
    // error: (assignment)
    builder().foo(null).build();
  }
}
