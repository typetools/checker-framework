// Test case for issue 370:
// https://code.google.com/p/checker-framework/issues/detail?id=370

import java.util.*;

//@skip-test
// TODO: This test only works as expected without the
// unsound untyped behavior, which we currently enable for test
// cases. Find a better place to put this.
class Issue370 {

  <T> Iterable<T> foo() {
    return Collections.<T>emptyList();
  }

}
