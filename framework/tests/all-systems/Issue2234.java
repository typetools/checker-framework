// Test case for Issue #2234:
// https://github.com/typetools/checker-framework/issues/2234

import java.util.LinkedList;
import java.util.List;

class Issue2234Super<T> {
  Issue2234Super(List<Integer> p) {}
}

@SuppressWarnings("unchecked") // raw supertype
class Issue2234Sub extends Issue2234Super {
  Issue2234Sub() {
    super(new LinkedList<>());
  }
}
