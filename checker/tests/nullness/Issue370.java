// Test case for issue 370:
// https://code.google.com/p/checker-framework/issues/detail?id=370

import java.util.*;

class Issue370 {

  <T> Iterable<T> foo() {
    return Collections.<T>emptyList();
  }

}
