import org.checkerframework.common.util.report.qual.*;

public class CallOverrides {
  class A {
    void m() {}
  }

  class B extends A {
    @ReportCall
    void m() {}
  }

  class C extends B {}

  void test() {
    C c = new C();

    // :: error: (methodcall)
    c.m();

    B b = c;

    // :: error: (methodcall)
    b.m();

    A a = c;

    // This call is not reported, because we statically
    // don't know that one of the subtypes has the ReportCall
    // annotation.
    a.m();
  }
}
