// Test case for Issue 2629
// https://github.com/typetools/checker-framework/issues/2629

import org.checkerframework.checker.index.qual.LessThan;

public class Issue2629 {
  @LessThan("#1 + 1") int test(int a) {
    return a;
  }
}
