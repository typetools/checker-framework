// Test case for issue #2156:
// https://github.com/typetools/checker-framework/issues/2156

// @skip-test until the bug is fixed

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

enum SampleEnum {
  @Untainted FIRST,
  @Tainted SECOND;
}

public class Issue2156 {
  void test() {
    requireUntainted(SampleEnum.FIRST);
    // :: error: assignment
    requireUntainted(SampleEnum.SECOND);
  }

  void requireUntainted(@Untainted SampleEnum sEnum) {}
}
