import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class OwningParams {
  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  private class OwningParamsDirectCall {
    void passOwnership(Foo f) {
      f.a();
    }

    void passOwnershipTest() {
      // :: warning: (required.method.not.called)
      Foo f = new Foo();
      passOwnership(f);
    }
  }

  private class OwningParamsIndirectCall {
    @EnsuresCalledMethods(
        value = {"#1"},
        methods = {"a"})
    void hasECM(Foo f) {
      f.a();
    }

    void owningFoo(@Owning Foo f) {
      f.a();
    }

    void passOwnership(Foo f1, Foo f2) {
      hasECM(f1);
      owningFoo(f2);
    }

    void passOwnershipTest() {
      // :: warning: (required.method.not.called)
      Foo f1 = new Foo();
      // :: warning: (required.method.not.called)
      Foo f2 = new Foo();
      passOwnership(f1, f2);
    }
  }
}
