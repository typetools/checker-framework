// Test case for issue 370:
// https://github.com/typetools/checker-framework/issues/370

import java.util.Collections;

public class Issue370 {

  <T> Iterable<T> foo() {
    return Collections.<T>emptyList();
  }
}
