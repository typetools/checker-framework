// a test case that @Owning @MustCallUnknown fields do not cause a crash. Of course,
// such fields should never exist. But, better to be safe. Relevant GitHub issue with
// discussion: https://github.com/typetools/checker-framework/issues/6030.

import org.checkerframework.checker.mustcall.qual.*;

public class OwningMCU {

  @Owning @MustCallUnknown Object foo;

  // :: error: missing.creates.mustcall.for
  void test() {
    foo = new Object();
  }
}
