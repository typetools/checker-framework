import org.checkerframework.checker.tainting.qual.Untainted;

/**
 * Resolution of outer classes must take substitution of generic types into account. Thanks to EMS
 * for finding this problem.
 *
 * <p>Also see all-systems/GenericsEnclosing for the type-system independent test.
 */
class MyG<X> {
  X f;

  void m(X p) {}
}

class ExtMyG extends MyG<@Untainted Object> {
  class EInner1 {
    class EInner2 {
      void bar() {
        // :: error: (assignment)
        f = 1;
        m("test");
        // :: error: (argument)
        m(1);
      }
    }
  }
}
