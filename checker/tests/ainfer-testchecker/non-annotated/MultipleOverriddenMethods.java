import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

/**
 * When a method overrides more than one method, the type inferred for one overridden method must
 * not affect the type inferred for the other overridden methods.
 */
public class MultipleOverriddenMethods {

  interface Iface1 {
    int m();
  }

  interface Iface2 {
    int m();
  }

  /** Makes Iface1.m() be inferred as returning @AinferParent, but says nothing about Iface2.m(). */
  static class OnlyIface1 implements Iface1 {
    @Override
    public int m() {
      return getAinferSibling2();
    }

    private @AinferSibling2 int getAinferSibling2() {
      return 0;
    }
  }

  /** The only implementation of Iface2.m(), so Iface2.m() is inferred to return @AinferSibling1. */
  static class BothIfaces implements Iface1, Iface2 {
    @Override
    public int m() {
      return getAinferSibling1();
    }

    private @AinferSibling1 int getAinferSibling1() {
      return 0;
    }
  }

  void test(Iface2 i2) {
    // :: warning: [argument]
    expectsAinferSibling1(i2.m());
  }

  void expectsAinferSibling1(@AinferSibling1 int t) {}
}
