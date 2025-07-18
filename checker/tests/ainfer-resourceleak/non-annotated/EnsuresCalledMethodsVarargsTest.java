import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class EnsuresCalledMethodsVarargsTest {

  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  static class Utils {
    @SuppressWarnings("ensuresvarargs.unverified")
    @EnsuresCalledMethodsVarargs("a")
    public static void close(Foo... foos) {
      for (Foo f : foos) {
        if (f != null) {
          f.a();
        }
      }
    }
  }

  private class ECMVA {
    final Foo foo;

    ECMVA() {
      // :: warning: (required.method.not.called)
      foo = new Foo();
    }

    void finalyzer() {
      Utils.close(foo);
    }

    @EnsuresCalledMethods(
        value = {"#1"},
        methods = {"a"})
    void closef(Foo f) {
      if (f != null) {
        Utils.close(f);
      }
    }

    void owningParam(Foo f) {
      Foo foo = f;
      Utils.close(foo);
    }

    void testOwningParamOnOwningParam() {
      // :: warning: (required.method.not.called)
      Foo f = new Foo();
      owningParam(f);
    }
  }

  void testCorrect() {
    ECMVA e = new ECMVA();
    e.finalyzer();
  }
}
