import org.checkerframework.framework.testchecker.testaccumulation.qual.*;

public class Xor {

  class Foo {
    void a() {}

    void b() {}

    void c() {}
    // use a standard gate encoding for xor
    void aXorB(@TestAccumulationPredicate("(a || b) && !(a && b)") Foo this) {}
  }

  void test1(Foo f) {
    // :: error: method.invocation.invalid
    f.aXorB();
  }

  void test2(Foo f) {
    f.c();
    // :: error: method.invocation.invalid
    f.aXorB();
  }

  void test3(Foo f) {
    f.a();
    f.aXorB();
  }

  void test4(Foo f) {
    f.b();
    f.aXorB();
  }

  void test5(Foo f) {
    f.a();
    f.b();
    // :: error: method.invocation.invalid
    f.aXorB();
  }

  void callA(Foo f) {
    f.a();
  }

  void test6(Foo f) {
    callA(f);
    f.b();
    // DEMONSTRATION OF "UNSOUNDNESS"
    f.aXorB();
  }

  void test7(@TestAccumulation("a") Foo f) {
    f.aXorB();
  }

  void test8(Foo f) {
    callA(f);
    // THIS IS AN UNAVOIDABLE FALSE POSITIVE
    // :: error: method.invocation.invalid
    f.aXorB();
  }
}
