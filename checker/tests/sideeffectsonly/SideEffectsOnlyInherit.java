// A method that overrides methods in two supertypes inherits the union of their
// `@SideEffectsOnly` expressions, not just the first supertype's.  The `value` element of
// `@SideEffectsOnly` is significant, unlike that of the other inherited declaration annotations.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.qual.EnsuresQualifier;

public class SideEffectsOnlyInherit {

  static class Cell {
    @Tainted Object f;
    @Tainted Object g;
  }

  interface I {
    @SideEffectsOnly("#1.f")
    void m(Cell c);
  }

  static class Base {
    @SideEffectsOnly("#1.g")
    public void m(Cell c) {}
  }

  // `C.m` cannot satisfy both supertype specifications; the override errors are suppressed in
  // order to test what `C.m` inherits.  Whichever supertype `AnnotatedTypes.overriddenMethods`
  // yields first, `C.m` is treated as side-effecting both `#1.f` and `#1.g`.
  @SuppressWarnings("purity.sideeffectsonly.overriding")
  static class C extends Base implements I {
    @Override
    public void m(Cell c) {}
  }

  @EnsuresQualifier(expression = "#1.f", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  static void makeFUntainted(Cell c) {}

  @EnsuresQualifier(expression = "#1.g", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  static void makeGUntainted(Cell c) {}

  static void testF(C receiver, Cell c) {
    makeFUntainted(c);
    receiver.m(c);
    // :: error: assignment
    @Untainted Object y = c.f;
  }

  static void testG(C receiver, Cell c) {
    makeGUntainted(c);
    receiver.m(c);
    // :: error: assignment
    @Untainted Object y = c.g;
  }

  // A `@SideEffectsOnly` written on the method itself is authoritative: nothing is inherited.
  @SuppressWarnings("purity.sideeffectsonly.overriding")
  static class D extends Base implements I {
    @SideEffectsOnly("#1.f")
    @Override
    public void m(Cell c) {}
  }

  static void testOwnAnnotationWins(D receiver, Cell c) {
    makeGUntainted(c);
    receiver.m(c);
    @Untainted Object y = c.g;
  }
}
