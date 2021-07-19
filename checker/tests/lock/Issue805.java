// Test case for Issue 805:
// https://github.com/typetools/checker-framework/issues/805

import org.checkerframework.checker.lock.qual.Holding;

public class Issue805 {
  @Holding("this.Issue805.class")
  // :: error: (flowexpr.parse.error)
  void method() {}

  @Holding("Issue805.class")
  void method2() {}

  @Holding("java.lang.String.class")
  void method3() {}
}
