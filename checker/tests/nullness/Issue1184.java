// Test case for https://github.com/typetools/checker-framework/issues/1184

import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue1184 {

  static void staticMethod() {}

  void instanceMethod() {}

  static void callStaticViaNullLocal() {
    Issue1184 obj = null;
    obj.staticMethod();
  }

  static void callStaticViaNullable(@Nullable Issue1184 obj) {
    obj.staticMethod();
  }

  static void callInstanceViaNullable(@Nullable Issue1184 obj) {
    // :: error: (dereference.of.nullable)
    obj.instanceMethod();
  }
}
