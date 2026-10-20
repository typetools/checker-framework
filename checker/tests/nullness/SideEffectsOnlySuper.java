// A `@SideEffectsOnly("this")` method that is called via `super` side-effects the object that the
// caller denotes as `this`, so refinements of that object's fields must be discarded.

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class SideEffectsOnlySuper {

  static class Super {
    @Nullable Object f;

    @SideEffectsOnly("this")
    void clear() {
      f = null;
    }
  }

  static class Sub extends Super {
    void viaSuper() {
      f = new Object();
      super.clear();
      // :: error: (dereference.of.nullable)
      f.toString();
    }

    void viaThis() {
      f = new Object();
      this.clear();
      // :: error: (dereference.of.nullable)
      f.toString();
    }
  }
}
