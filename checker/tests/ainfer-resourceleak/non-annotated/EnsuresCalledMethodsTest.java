import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class EnsuresCalledMethodsTest {
  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  private class ECM {
    Foo foo;

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

  //  static void test() {
  //    // :: error: required.method.not.called
  //    Foo foo = new Foo();
  //    ECM ecm = new ECM();
  //    ecm.foo = foo;
  //    ecm.close();
  //  }

}
