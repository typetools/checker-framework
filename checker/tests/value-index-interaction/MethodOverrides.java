// This class should not issues any errors from the value checker.
// The index checker should issue the errors instead.

// There is a copy of this test at checker/tests/index/MethodOverrides.java,
// which includes expected failures.

import org.checkerframework.checker.index.qual.GTENegativeOne;

public class MethodOverrides {
  @GTENegativeOne int read() {
    return -1;
  }
}

class MethodOverrides2 extends MethodOverrides {
  int read() {
    return -1;
  }
}
