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

  void test1() {
    Foo f = new Foo();
    f.b1();
    f.a();
  }

  void test2() {
    Foo f = new Foo();
    f.b2();
    f.a();
  }

  void test3() {
    Foo f = new Foo();
    Foo ff = f.b1();
    ff.a();
  }

  void test4() {
    Foo f = new Foo();
    Foo ff = f.b2();
    ff.a();
  }
}
