import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class OverWriteOwningField {

  @InheritableMustCall("a")
  static class Foo {
    void a() {}

    void c() {}
  }

  private class SubClass extends Foo {
    @SuppressWarnings("required.method.not.called")
    Foo f;

    SubClass() {
      // :: warning: (required.method.not.called)
      f = new Foo();
    }

    @CreatesMustCallFor
    void reWrite() {
      f.a();
      // :: warning: (required.method.not.called)
      f = new Foo();
    }

    void a() {
      Utils.closef(this.f);
    }
  }

  static class Utils {
    public static void closef(Foo f) {
      if (f != null) {
        f.a();
      }
    }
  }
}
