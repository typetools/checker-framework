// A test for a bad interaction between CO and subtyping
// that could happen if CO was unsound.

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class COInSubtype {
  static class Foo {

    @CreatesMustCallFor("this")
    void resetFoo() {}
  }

  @MustCall("a") static class Bar extends Foo {
    void a() {}
  }

  static void test() {
    // :: error: required.method.not.called
    @MustCall("a") Foo f = new Bar();
    f.resetFoo();
  }
}
