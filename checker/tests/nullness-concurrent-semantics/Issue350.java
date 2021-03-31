// Test case for Issue 350:
// https://github.com/typetools/checker-framework/issues/350

import org.checkerframework.checker.nullness.qual.*;

class Test1 {

  public @Nullable String y;

  public void test2() {
    y = "";
    // Sanity check that -AconcurrentSemantics is set
    // :: error: (dereference.of.nullable)
    y.toString();
  }

  private @MonotonicNonNull String x;

  void test() {
    if (x == null) {
      x = "";
    }
    x.toString();
  }
}

class Test2 {

  private @MonotonicNonNull String x;

  void setX(String x) {
    this.x = x;
  }

  void test() {
    if (x == null) {
      x = "";
    }
    setX(x);
  }
}

class Test3 {

  private @MonotonicNonNull String x;

  @EnsuresNonNull("#1")
  void setX(final String x) {
    this.x = x;
  }
}
