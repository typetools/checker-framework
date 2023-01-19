import org.checkerframework.checker.mustcall.qual.*;

class OwningField {

  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  @InheritableMustCall("dispose")
  static class FinalOwningField {
    final Foo f;

    FinalOwningField() {
      // :: warning: (required.method.not.called)
      f = new Foo();
    }

    void dispose() {
      f.a();
    }
  }
}
