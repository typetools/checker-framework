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
    private final Foo finalNotOwningFoo;

    public FooField() {
      this.finalOwningFoo = new Foo();
      // :: error: required.method.not.called
      this.finalNotOwningFoo = new Foo();
    }

    @EnsuresCalledMethods(
        value = {"this.finalOwningFoo"},
        methods = {"a"})
    void b() {
      this.finalOwningFoo.a();
      this.finalOwningFoo.c();
    }

    void b2() {
      try {
        this.finalNotOwningFoo.c();
        throw new Exception();
      } catch (Exception io) {
        this.finalNotOwningFoo.a();
      }
    }
  }

  void testField() {
    FooField fooField = new FooField();
    fooField.b();
  }
}
