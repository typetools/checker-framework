// Tests that the @Owning parameters of the superclass constructor of an anonymous class are still
// checked.

import org.checkerframework.checker.mustcall.qual.*;

class OwningParamsAnonymous {
  static class Nested {
    Nested(@Owning OwningParamsAnonymous o) {}
  }

  class Inner {
    Inner(@Owning OwningParamsAnonymous o) {}
  }

  void test(
      OwningParamsAnonymous outer,
      @Owning @MustCall({"a"}) OwningParamsAnonymous o,
      @Owning OwningParamsAnonymous p) {
    // :: error: [argument]
    new Nested(o);
    // :: error: [argument]
    new Nested(o) {};
    // :: error: [argument]
    new Inner(o) {};
    // :: error: [argument]
    outer.new Inner(o) {};
    new Nested(p) {};
    new Inner(p) {};
    outer.new Inner(p) {};
  }
}
