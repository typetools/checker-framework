// Test case for Issue 1817:
// https://github.com/typetools/checker-framework/issues/1817

import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("all") // only check for crashes
public class Issue1817 {
  {
    Consumer<List<?>> c = values -> values.forEach(value -> f(value));
  }

  void f(Object o) {}
}
