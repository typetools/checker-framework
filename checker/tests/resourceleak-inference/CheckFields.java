import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.Owning;

@MustCall("finalizer") class CheckFields {
  private final @Owning Foo checkFieldsFoo;

  public CheckFields() {
    this.checkFieldsFoo = new Foo();
  }

  @EnsuresCalledMethods(
      value = {"this.checkFieldsFoo"},
      methods = {"a"})
  void finalizer() {
    this.checkFieldsFoo.a();
  }

  @MustCall("a") static class Foo {
    void a() {}

    void c() {}
  }

  Foo makeFoo() {
    return new Foo();
  }

  @MustCall("b") static class FooField {
    private final @Owning Foo finalOwningFoo;

    public FooField() {
      this.finalOwningFoo = new Foo();
    }

    @EnsuresCalledMethods(
        value = {"this.finalOwningFoo"},
        methods = {"a"})
    void b() {
      this.finalOwningFoo.a();
      this.finalOwningFoo.c();
    }
  }

  void testField() {
    FooField fooField = new FooField();
    fooField.b();
  }
}
