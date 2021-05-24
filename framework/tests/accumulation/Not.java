import org.checkerframework.framework.testchecker.testaccumulation.qual.*;

public class Not {

  class Foo {
    void a() {}

    void b() {}

    void c() {}

    void notA(@TestAccumulationPredicate("!a") Foo this) {}

    void notB(@TestAccumulationPredicate("!b") Foo this) {}
  }

  void test1(Foo f) {
    f.notA();
    f.notB();
  }

  void test2(Foo f) {
    f.c();
    f.notA();
    f.notB();
  }

  void test3(Foo f) {
    f.a();
    // :: error: method.invocation
    f.notA();
    f.notB();
  }

  void test4(Foo f) {
    f.b();
    f.notA();
    // :: error: method.invocation
    f.notB();
  }

  void test5(Foo f) {
    f.a();
    f.b();
    // :: error: method.invocation
    f.notA();
    // :: error: method.invocation
    f.notB();
  }

  void callA(Foo f) {
    f.a();
  }

  void test6(Foo f) {
    callA(f);
    // DEMONSTRATION OF "UNSOUNDNESS"
    f.notA();
  }

  void test7(@TestAccumulation("a") Foo f) {
    // :: error: method.invocation
    f.notA();
  }

  void test8(Foo f, boolean test) {
    if (test) {
      f.a();
    } else {
      f.b();
    }
    // DEMONSTRATION OF "UNSOUNDNESS"
    f.notA();
  }
}
