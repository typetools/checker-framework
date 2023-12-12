// Tests for @PolyMustCall and @MustCallAlias where the must-call method of the return type has
// a different name than the must-call method of the parameter type.

import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.*;

class PolyMustCallDifferentNames {

  @InheritableMustCall("a")
  static class Wrapped {
    void a() {}
  }

  @InheritableMustCall("b")
  static class Wrapper1 {
    private final @Owning Wrapped field;

    public @PolyMustCall Wrapper1(@PolyMustCall Wrapped w) {
      // we get this error since we only have a field-assignment special case for @MustCallAlias,
      // not @PolyMustCall
      // :: error: (assignment)
      this.field = w;
    }

    @EnsuresCalledMethods(
        value = {"this.field"},
        methods = {"a"})
    void b() {
      this.field.a();
    }
  }

  static @PolyMustCall Wrapper1 getWrapper1(@PolyMustCall Wrapped w) {
    return new Wrapper1(w);
  }

  @InheritableMustCall("c")
  static class Wrapper2 {
    private final @Owning Wrapped field;

    public @MustCallAlias Wrapper2(@MustCallAlias Wrapped w) {
      this.field = w;
    }

    @EnsuresCalledMethods(
        value = {"this.field"},
        methods = {"a"})
    void c() {
      this.field.a();
    }
  }

  static @MustCallAlias Wrapper2 getWrapper2(@MustCallAlias Wrapped w) {
    return new Wrapper2(w);
  }

  static void test1() {
    @MustCall("a") Wrapped x = new Wrapped();
    @MustCall("b") Wrapper1 w1 = new Wrapper1(x);
    @MustCall("b") Wrapper1 w2 = getWrapper1(x);
    // :: error: (assignment)
    @MustCall("a") Wrapper1 w3 = new Wrapper1(x);
    // :: error: (assignment)
    @MustCall("a") Wrapper1 w4 = getWrapper1(x);
  }

  static void test2() {
    @MustCall("a") Wrapped x = new Wrapped();
    @MustCall("c") Wrapper2 w1 = new Wrapper2(x);
    @MustCall("c") Wrapper2 w2 = getWrapper2(x);
    // :: error: (assignment)
    @MustCall("a") Wrapper2 w3 = new Wrapper2(x);
    // :: error: (assignment)
    @MustCall("a") Wrapper2 w4 = getWrapper2(x);
  }
}
