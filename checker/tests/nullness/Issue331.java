// Test case for Issue 331:
// https://github.com/typetools/checker-framework/issues/331

import java.util.List;

class TestTeranry {
  void foo(boolean b, List<Object> res) {
    Object o = b ? "x" : (b ? "y" : "z");
    res.add(o);
  }
}
