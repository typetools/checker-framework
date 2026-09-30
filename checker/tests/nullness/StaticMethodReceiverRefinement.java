// Calling a static method through a possibly-null reference does not dereference the reference, so
// it must not refine the reference to non-null.

import org.checkerframework.checker.nullness.qual.Nullable;

public class StaticMethodReceiverRefinement {
  static void staticMethod() {}

  void instanceMethod() {}

  static void nullLocal() {
    StaticMethodReceiverRefinement obj = null;
    obj.staticMethod();
    // :: error: (dereference.of.nullable)
    obj.instanceMethod();
  }

  static void nullableParam(@Nullable StaticMethodReceiverRefinement obj) {
    obj.staticMethod();
    // :: error: (dereference.of.nullable)
    obj.instanceMethod();
  }

  static void instanceCall(@Nullable StaticMethodReceiverRefinement obj) {
    // :: error: (dereference.of.nullable)
    obj.instanceMethod();
    obj.instanceMethod();
  }
}
