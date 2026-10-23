// Without -AcheckPurityAnnotations, an overriding method is not checked against the overridden
// method's `@SideEffectsOnly` annotation, so no diagnostic is issued.  This file holds violations
// that checker/tests/sideeffectsonly/SideEffectsOnlyOverride.java asserts.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class SideEffectsOnlyOverrideNotChecked {

  static class Cell {
    Object g;
    Cell inner;
  }

  static class Super {
    @SideEffectsOnly("#1.inner")
    void m(Cell c) {}
  }

  /** Side-effects more than the supertype permits. */
  static class SubMore extends Super {
    @SideEffectsOnly({"#1.inner", "#1.g"})
    @Override
    void m(Cell c) {}
  }

  interface I {
    @SideEffectsOnly("#1.g")
    void m(Cell c);
  }

  /** Inherits the union of what two supertypes permit, which is more than either one permits. */
  static class SubOfTwo extends Super implements I {
    @Override
    public void m(Cell c) {}
  }
}
