// Test case to check that a wrapper type can have a @MustCall method with a different name than
// the @MustCall method of the type it wraps.
// See https://github.com/typetools/checker-framework/issues/4947

import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.checker.mustcall.qual.Owning;

class MustCallAliasDifferentMethodNames {

  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  @InheritableMustCall("b")
  static class FooField {
    private final @Owning Foo finalOwningFoo;

    public @MustCallAlias FooField(@MustCallAlias Foo f) {
      this.finalOwningFoo = f;
    }

    @EnsuresCalledMethods(
        value = {"this.finalOwningFoo"},
        methods = {"a"})
    void b() {
      this.finalOwningFoo.a();
    }
  }

  void testField1() {
    Foo f = new Foo();
    FooField fooFieldWrapper = new FooField(f);
    // Either calling f.a() or fooFieldWrapper.b() satisfies the obligation
    fooFieldWrapper.b();
  }

  void testField2() {
    Foo f = new Foo();
    FooField fooFieldWrapper = new FooField(f);
    // Either calling f.a() or fooFieldWrapper.b() satisfies the obligation
    f.a();
  }
}
