// An `instanceof` whose expression is a method call refines the store, just as one whose
// expression is a local variable does.

import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.Pure;

public class InstanceOfAfterMethodCall {

  @Untainted Object untainted() {
    return "";
  }

  @Pure
  Object tainted() {
    return "";
  }

  void requireUntainted(@Untainted Object o) {}

  void bindingFromLocal(@Untainted Object o) {
    if (o instanceof String s) {
      requireUntainted(s);
    }
  }

  void bindingFromMethodCall() {
    if (untainted() instanceof String s) {
      requireUntainted(s);
    }
  }

  void refineLocal(Object o) {
    // :: warning: (instanceof.unsafe)
    if (o instanceof @Untainted String) {
      requireUntainted(o);
    }
  }

  void refineMethodCall() {
    // :: warning: (instanceof.unsafe)
    if (tainted() instanceof @Untainted String) {
      requireUntainted(tainted());
    }
  }
}
