// Test case for issue #428:
// https://github.com/typetools/checker-framework/issues/428

import java.util.List;

interface Issue428<T extends Number> {}

class Test {
  void m(List<Issue428<? extends Object>> is) {
    Issue428<?> i = is.get(0);
  }
}
