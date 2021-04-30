import org.checkerframework.checker.regex.qual.*;

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

class ExtMyG extends MyG<@Regex String> {
  class EInner1 {
    class EInner2 {
      void bar() {
        String s = f;
        f = "hi";
        // :: error: (assignment)
        f = "\\ no regex(";

        m("hi!");
        // :: error: (argument)
        m("\\ no regex(");
      }
    }
  }
}
