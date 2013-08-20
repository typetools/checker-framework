// Test case for issue #238: https://code.google.com/p/checker-framework/issues/detail?id=238

import java.util.*;

class TestInfer {
  <T> T getValue(List<T> l) {
    return l.get(0);
  }
  void bar(Object o) { }
  void foo() {
    List<?> ls = new ArrayList<>();
    bar(getValue(ls));
  }
}
