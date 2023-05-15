import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class EnsuresCalledMethodsVarArgsTest {

  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  static class Utils {
    @SuppressWarnings("ensuresvarargs.unverified")
    @EnsuresCalledMethodsVarArgs("a")
    public static void close(Foo... foos) {
      for (Foo f : foos) {
        if (f != null) {
          f.a();
        }
      }
    }
  }

  private class ECMVA {
    final Foo fff;

    ECMVA() {
      // :: warning: (required.method.not.called)
      fff = new Foo();
    }

    void finalyzer() {
      Utils.close(fff);
    }

    @EnsuresCalledMethods(
        value = {"#1"},
        methods = {"a"})
    void closef(Foo f) {
      if (f != null) {
        Utils.close(f);
      }
    }
  }

  void testCorrect() {
    ECMVA e = new ECMVA();
    e.finalyzer();
  }
}
