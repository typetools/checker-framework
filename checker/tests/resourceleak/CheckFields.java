import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

class CheckFields {

  @MustCall("a") static class Foo {
    void a() {}

    void c() {}
  }

  Foo makeFoo() {
    return new Foo();
  }

  @MustCall("b") static class FooField {
    private final @Owning Foo finalOwningFoo;
    // :: error: required.method.not.called
    private final @Owning Foo finalOwningFooWrong;
    private final Foo finalNotOwningFoo;
    private @Owning Foo owningFoo;
    private @Owning @MustCall({}) Foo owningEmptyMustCallFoo;
    private Foo notOwningFoo;

    public FooField() {
      this.finalOwningFoo = new Foo();
      this.finalOwningFooWrong = new Foo();
      // :: error: required.method.not.called
      this.finalNotOwningFoo = new Foo();
    }

    @CreatesObligation
    void assingToOwningFieldWrong() {
      Foo f = new Foo();
      // :: error: required.method.not.called
      this.owningFoo = f;
    }

    @CreatesObligation
    void assignToOwningFieldWrong2() {
      // :: error: required.method.not.called
      this.owningFoo = new Foo();
    }

    @CreatesObligation
    void assingToOwningField() {
      // this is a safe re-assignment.
      if (this.owningFoo == null) {
        Foo f = new Foo();
        this.owningFoo = f;
      }
    }

    void assingToFinalNotOwningField() {
      // :: error: required.method.not.called
      Foo f = new Foo();
      this.notOwningFoo = f;
    }

    Foo getOwningFoo() {
      return this.owningFoo;
    }

    @EnsuresCalledMethods(
        value = {"this.finalOwningFoo", "this.owningFoo"},
        methods = {"a"})
    void b() {
      this.finalOwningFoo.a();
      this.finalOwningFoo.c();
      this.owningFoo.a();
    }
  }

  void testField() {
    FooField fooField = new FooField();
    fooField.b();
  }

  void testAccessField() {
    FooField fooField = new FooField();
    // :: error: required.method.not.called
    fooField.owningFoo = new Foo();
    fooField.b();
  }

  void testAccessField2() {
    FooField fooField = new FooField();
    if (fooField.owningFoo == null) {
      fooField.owningFoo = new Foo();
    }
    fooField.b();
  }

  void testAccessFieldWrong() {
    // :: error: required.method.not.called
    FooField fooField = new FooField();
    // :: error: required.method.not.called
    fooField.owningFoo = new Foo();
    // :: error: required.method.not.called
    fooField.notOwningFoo = new Foo();
  }

  @CreatesObligation("#1")
  void testAccessField_param(FooField fooField) {
    // :: error: required.method.not.called
    fooField.owningFoo = new Foo();
    fooField.b();
  }

  // :: error: missing.creates.obligation
  void testAccessField_param_no_co(FooField fooField) {
    // :: error: required.method.not.called
    fooField.owningFoo = new Foo();
    fooField.b();
  }

  static class NestedWrong {

    // Non-final owning fields also require the surrounding class to have an appropriate MC
    // annotation.
    // :: error: required.method.not.called
    @Owning Foo foo;

    @CreatesObligation("this")
    void initFoo() {
      if (this.foo == null) {
        this.foo = new Foo();
      }
    }
  }

  @MustCall("f") static class NestedWrong2 {
    // Non-final owning fields also require the surrounding class to have an appropriate MC
    // annotation.
    // :: error: required.method.not.called
    @Owning Foo foo;

    @CreatesObligation("this")
    void initFoo() {
      if (this.foo == null) {
        this.foo = new Foo();
      }
    }

    void f() {}
  }

  @MustCall("f") static class NestedRight {
    // Non-final owning fields also require the surrounding class to have an appropriate MC
    // annotation.
    @Owning Foo foo;

    @CreatesObligation("this")
    void initFoo() {
      if (this.foo == null) {
        this.foo = new Foo();
      }
    }

    @EnsuresCalledMethods(value = "this.foo", methods = "a")
    void f() {
      this.foo.a();
    }
  }
}
