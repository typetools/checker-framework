// An overriding method must not side-effect more than the overridden method's
// `@SideEffectsOnly` annotation permits.  Otherwise a call whose receiver is statically of the
// supertype would retain a refinement that the override invalidates.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.qual.EnsuresQualifier;

public class SideEffectsOnlyOverride {

  static class Cell {
    @Tainted Object g;
    @Tainted Cell inner;
  }

  static class Super {
    @SideEffectsOnly("#1.inner")
    void m(Cell c) {}
  }

  /** Side-effects exactly what the supertype permits. */
  static class SubSame extends Super {
    @SideEffectsOnly("#1.inner")
    @Override
    void m(Cell c) {}
  }

  /** Side-effects less than the supertype permits: `#1.inner.g` is reached through `#1.inner`. */
  static class SubDeeper extends Super {
    @SideEffectsOnly("#1.inner.g")
    @Override
    void m(Cell c) {}
  }

  /** Side-effects nothing at all. */
  static class SubSideEffectFree extends Super {
    @SideEffectFree
    @Override
    void m(Cell c) {}
  }

  /** Side-effects more than the supertype permits. */
  static class SubMore extends Super {
    @SideEffectsOnly({"#1.inner", "#1.g"})
    @Override
    // TODO :: error: purity.sideeffectsonly.overriding
    void m(Cell c) {}
  }

  /** `#1` is not reached through `#1.inner`, so side-effecting it is more than permitted. */
  static class SubWhole extends Super {
    @SideEffectsOnly("#1")
    @Override
    // TODO :: error: purity.sideeffectsonly.overriding
    void m(Cell c) {}
  }

  /** A supertype without `@SideEffectsOnly` constrains nothing. */
  static class Unconstrained {
    void m(Cell c) {}
  }

  static class SubOfUnconstrained extends Unconstrained {
    @SideEffectsOnly("#1.inner")
    @Override
    void m(Cell c) {}
  }

  @EnsuresQualifier(expression = "#1.g", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  static void makeUntainted(Cell c) {}

  /**
   * Without the override check, this refinement would be wrongly retained: the call resolves
   * statically to {@code Super.m}, which does not permit side-effecting {@code c.g}, but it may
   * execute {@code SubMore.m}, which does.
   */
  static void testDynamicDispatch(Super s, Cell c) {
    makeUntainted(c);
    s.m(c);
    @Untainted Object y = c.g;
  }
}
