// @skip-test until the bug is fixed

import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCallAlias;
import org.checkerframework.common.returnsreceiver.qual.This;

class RlcThisTest {

  @InheritableMustCall("a")
  private class Foo {

    void a() {}

    @This Foo b1(Foo this) {
      return this;
    }

    @MustCallAlias Foo b2(@MustCallAlias Foo this) {
      return this;
    }
  }

  void test() {
    Foo f1 = new Foo();
    f1.b1(); // RLC reports a FP at this line
    f1.a();

    Foo f2 = new Foo();
    f2.b2(); // RLC reports a FP at this line
    f2.a();

    f1 = new Foo();
    Foo ff1 = f1.b1();
    ff1.a();

    f2 = new Foo();
    Foo ff2 = f2.b2();
    ff2.a();
  }
}
