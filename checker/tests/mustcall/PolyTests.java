// Unit tests for the poly annotation.

import org.checkerframework.checker.mustcall.qual.*;

@MustCall("close") class PolyTests {
  static @PolyMustCall Object id(@PolyMustCall Object obj) {
    return obj;
  }

  static void test1(@Owning @MustCall("close") Object o) {
    @MustCall("close") Object o1 = id(o);
    // :: error: assignment.type.incompatible
    @MustCall({}) Object o2 = id(o);
  }

  static void test2(@Owning @MustCall({}) Object o) {
    @MustCall("close") Object o1 = id(o);
    @MustCall({}) Object o2 = id(o);
  }

  // These sort of constructors will always appear in stub files and are unverifiable for now.
  @SuppressWarnings("mustcall:type.invalid.annotations.on.use")
  @PolyMustCall PolyTests(@PolyMustCall Object obj) {}

  static void test3(@Owning @MustCall({"close"}) Object o) {
    @MustCall("close") Object o1 = new PolyTests(o);
    // :: error: assignment.type.incompatible
    @MustCall({}) Object o2 = new PolyTests(o);
  }

  static void test4(@Owning @MustCall({}) Object o) {
    @MustCall("close") Object o1 = new PolyTests(o);
    @MustCall({}) Object o2 = new PolyTests(o);
  }

  static void testArbitary(@Owning PolyTests p) {
    @MustCall("close") Object o1 = p;
    // :: error: assignment.type.incompatible
    @MustCall({}) Object o2 = p;
  }
}
