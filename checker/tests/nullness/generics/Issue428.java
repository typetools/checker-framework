// Test case for issue #428:
// https://github.com/typetools/checker-framework/issues/428

import java.util.List;

public interface Issue428<T extends Number> {}

class Test428 {
  void m(List<Issue428<? extends Object>> is) {
    Issue428<?> i = is.get(0);
  }
}
