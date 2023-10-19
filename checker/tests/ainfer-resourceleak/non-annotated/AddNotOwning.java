import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class AddNotOwning {

  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  static class NonEmptyMustCallFinalField {
    final Foo f; // expect owning annotation for this field

    NonEmptyMustCallFinalField() {
      // :: warning: (required.method.not.called)
      f = new Foo();
    }

    Foo getField() {
      return f;
    }

    void testNotOwningOnFinal() {
      // :: warning: (required.method.not.called)
      Foo f = getField();
    }

    @EnsuresCalledMethods(
        value = {"this.f"},
        methods = {"a"})
    void dispose() {
      f.a();
    }
  }

  @InheritableMustCall("dispose")
  static class NonEmptyMustCallNonFinalField {
    Foo f; // expect owning annotation for this field

    @SuppressWarnings("missing.creates.mustcall.for")
    void initialyzeFoo() {
      f.a();
      // :: warning: (required.method.not.called)
      f = new Foo();
    }

    Foo getField() {
      return f;
    }

    void testNotOwningOnNonFinal() {
      // :: warning: (required.method.not.called)
      Foo f = getField();
    }

    @EnsuresCalledMethods(
        value = {"this.f"},
        methods = {"a"})
    void dispose() {
      f.a();
    }
  }
}
