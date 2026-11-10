// An overriding method is checked against the overridden method's `@SideEffectsOnly` annotation
// under `-Ainfer`, even without `-AcheckPurityAnnotations`.  Unlike the check of a method body,
// the override check runs under any of `-AcheckPurityAnnotations`, `-AsuggestPureMethods`, and
// `-Ainfer`.  See also checker/tests/sideeffectsonly-nocheck/OverrideCheckedWithSuggestions.java.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class OverrideCheckedWithInfer {

  static class Cell {
    Object g;
    Cell inner;
  }

  static class Super {
    @SideEffectsOnly("#1.inner")
    void m(Cell c) {
      c.inner = null;
    }
  }

  /** Side-effects more than the supertype permits. */
  static class SubMore extends Super {
    @SideEffectsOnly({"#1.inner", "#1.g"})
    @Override
    // :: error: purity.sideeffectsonly.overriding
    void m(Cell c) {
      c.inner = null;
      c.g = null;
    }
  }
}
