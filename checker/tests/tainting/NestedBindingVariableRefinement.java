// @below-java21-jdk-skip-test

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

// A binding variable nested in a record pattern holds a record component, so it is not refined by
// the qualifiers of the value that instanceof tests.
public class NestedBindingVariableRefinement {

  record Box(@Tainted String s) {}

  void sink(@Untainted String s) {}

  void nested(@Untainted Object o) {
    if (o instanceof Box(String s)) {
      // :: error: [argument]
      sink(s);
    }
  }

  record UntaintedBox(@Untainted String s) {}

  // The binding variable is refined by the qualifiers of the record component.
  void nestedUntaintedComponent(@Tainted Object o) {
    if (o instanceof UntaintedBox(String s)) {
      sink(s);
    }
  }

  void accessor(@Untainted Object o) {
    if (o instanceof Box b) {
      // :: error: [argument]
      sink(b.s());
    }
  }

  void topLevel(@Untainted Object o) {
    if (o instanceof String s) {
      sink(s);
    }
  }
}
