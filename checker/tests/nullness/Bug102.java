// Test case for Issue 102
public final class Bug102 {
  class C<T extends @org.checkerframework.checker.nullness.qual.Nullable Object> {}

  void bug1() {
    C<String> c = new C<>();
    m(c);
    m(c); // note: the bug disapear if calling m only once
  }

  void bug2() {
    C<String> c = new C<>();
    m(c);
  }

  // :: error: (invalid.polymorphic.qualifier)
  <@org.checkerframework.checker.nullness.qual.PolyNull S> void m(
      final C<@org.checkerframework.checker.nullness.qual.PolyNull String> a) {}
}
