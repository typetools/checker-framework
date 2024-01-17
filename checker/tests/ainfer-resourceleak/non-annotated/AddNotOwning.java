// This test ensures that the @NotOwning annotation is inferred for the return type of a method if
// it returns a field.

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

    Foo getFieldOnSomePath() {
      if (true) {
        return null;
      } else {
        return f;
      }
    }

    void testNotOwningOnFinal() {
      // :: warning: (required.method.not.called)
      Foo f = getField();
    }

    void testNotOwningOnGetFieldOnSomePath() {
      // :: warning: (required.method.not.called)
      Foo f = getFieldOnSomePath();
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

    Foo getFieldOnSomePath() {
      if (true) {
        return null;
      } else {
        return f;
      }
    }

    void testNotOwningOnNonFinal() {
      // :: warning: (required.method.not.called)
      Foo f = getField();
    }

    void testNotOwningOnGetFieldOnSomePath() {
      // :: warning: (required.method.not.called)
      Foo f = getFieldOnSomePath();
    }

    @EnsuresCalledMethods(
        value = {"this.f"},
        methods = {"a"})
    void dispose() {
      f.a();
    }
  }
}
