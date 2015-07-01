// Test case for issue 411:
// https://code.google.com/p/checker-framework/issues/detail?id=411

// Skip the test until Issue 411 is fixed.
// @skip-test

import org.checkerframework.checker.nullness.qual.*;

class Test {

  @MonotonicNonNull Object field1 = null;
  final @Nullable Object field2 = null;

  void m() {
    if (field1 != null) {
      new Object() {
        void f() {
          field1.toString(); // dereference of possibly-null reference
        }
      };
    }
  }

  void n() {
    if (field2 != null) {
      new Object() {
        void f() {
          field2.toString(); // dereference of possibly-null reference
        }
      };
    }
  }
}
