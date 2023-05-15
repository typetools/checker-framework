import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class EnsuresCalledMethodsTest {
  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  private class ECM {
    Foo foo;

    //    private ECM() {
    //      foo = new Foo();
    //    }

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

  //  void testECM() {
  //    ECM e = new ECM();
  //    e.close();
  //  }
  //
  //  void testECMWrong() {
  //    // :: warning: (required.method.not.called)
  //    ECM e = new ECM();
  //  }
}
