// Under `-Ainfer` without `-AcheckPurityAnnotations`, an overriding method is not checked against
// the overridden method's `@SideEffectsOnly` annotation, so no diagnostic is issued.  Like the
// check of a method body against its own `@SideEffectsOnly` annotation, the override check runs
// only when `-AcheckPurityAnnotations` itself is supplied.  See also
// checker/tests/sideeffectsonly-nocheck/OverrideNotCheckedWithSuggestions.java.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class OverrideNotCheckedWithInfer {

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
    void m(Cell c) {
      c.inner = null;
      c.g = null;
    }
  }
}
