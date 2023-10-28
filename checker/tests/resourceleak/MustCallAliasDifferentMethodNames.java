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

  void testField() {
    Foo f = new Foo(); // False Positive for this line
    FooField fooField12345 = new FooField(f);
    fooField12345.b();
  }
}
