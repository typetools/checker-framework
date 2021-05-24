import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class Lazy {

  @NonNull String f;
  @MonotonicNonNull String g;
  @MonotonicNonNull String g2;
  @org.checkerframework.checker.nullness.qual.MonotonicNonNull String _g;
  @org.checkerframework.checker.nullness.qual.MonotonicNonNull String _g2;

  // Initialization with null is allowed for legacy reasons.
  @MonotonicNonNull String init = null;

  public Lazy() {
    f = "";
    // does not have to initialize g
  }

  void test() {
    g = "";
    test2(); // retain non-null property across method calls
    g.toLowerCase();
  }

  void _test() {
    _g = "";
    test2(); // retain non-null property across method calls
    _g.toLowerCase();
  }

  void test2() {}

  void test3() {
    // :: error: (dereference.of.nullable)
    g.toLowerCase();
  }

  void test4() {
    // :: error: (assignment)
    g = null;
    // :: error: (monotonic)
    g = g2;
  }

  void _test3() {
    // :: error: (dereference.of.nullable)
    _g.toLowerCase();
  }

  void _test4() {
    // :: error: (assignment)
    _g = null;
    // :: error: (monotonic)
    _g = _g2;
  }
}
