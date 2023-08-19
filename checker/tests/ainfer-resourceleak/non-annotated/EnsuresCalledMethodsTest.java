import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class EnsuresCalledMethodsTest {
  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  @InheritableMustCall("close")
  class ECM {
    // :: warning: (required.method.not.called)
    @Owning Foo foo;

    private void closePrivate() {
      if (foo != null) {
        foo.a();
        foo = null;
      }
    }

    void close() {
      if (foo != null) {
        foo.a();
        foo = null;
      }
    }
  }
}
