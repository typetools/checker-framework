// The expressions of an inherited `@SideEffectsOnly` annotation are resolved in the scope of the
// supertype method that declares them, not in the scope of the method that inherits them.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.qual.EnsuresQualifier;

public class SideEffectsOnlyInheritScope {

  static class Sup {
    @Tainted Object f;

    @SideEffectsOnly("this.f")
    void m() {}
  }

  static class Sub extends Sup {
    // Shadows `Sup.f`.  `Sub.m` inherits a specification about `Sup.f`, not about `Sub.f`.
    @Tainted Object f;

    @Override
    void m() {}
  }

  @EnsuresQualifier(expression = "#1.f", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  static void makeSupFUntainted(Sup s) {}

  @EnsuresQualifier(expression = "#1.f", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  static void makeSubFUntainted(Sub s) {}

  static void testShadowingFieldIsRetained(Sub s) {
    makeSubFUntainted(s);
    s.m();
    // `Sub.f` is not side-effected, so its refinement is retained.
    @Untainted Object y = s.f;
  }

  static void testShadowedFieldIsDiscarded(Sub s) {
    makeSupFUntainted(s);
    s.m();
    // :: error: assignment
    @Untainted Object y = ((Sup) s).f;
  }
}
