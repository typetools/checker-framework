// Test case for Issue 658:
// https://github.com/typetools/checker-framework/issues/658
// @skip-test

import org.checkerframework.common.util.report.qual.*;

public class Interface {
  interface A {
    @ReportCall
    boolean equals(Object o);

    @ReportCall
    void mine();
  }

  class B implements A {
    public void mine() {}
  }

  interface C extends A {}

  void foo(A a, B b, C c, Object o) {
    // :: error: (methodcall)
    if (a.equals(o)) {}
    // :: error: (methodcall)
    if (b.equals(o)) {}
    // :: error: (methodcall)
    if (c.equals(o)) {}

    // Don't report this call.
    if (o.equals(a)) {}
  }

  void bar(A a, B b, C c, Object o) {
    // :: error: (methodcall)
    a.mine();
    // :: error: (methodcall)
    b.mine();
    // :: error: (methodcall)
    c.mine();
  }
}
