// Inference of @Owning for a parameter that is passed to an @Owning parameter of the superclass
// constructor of an anonymous class.

import org.checkerframework.checker.mustcall.qual.*;

class AnonymousClassOwningParams {
  @InheritableMustCall("a")
  static class Foo {
    void a() {}
  }

  class Inner {
    Inner(@Owning Foo f) {
      f.a();
    }
  }

  class InnerVarargs {
    InnerVarargs(Foo... fs) {}
  }

  void passImplicitEnclosing(Foo f) {
    new Inner(f) {};
  }

  void passExplicitEnclosing(AnonymousClassOwningParams outer, Foo f) {
    outer.new Inner(f) {};
  }

  void passVarargs(AnonymousClassOwningParams outer, Foo f1, Foo f2) {
    outer.new InnerVarargs(f1, f2) {};
  }

  void passImplicitEnclosingTest() {
    // :: warning: [required.method.not.called]
    Foo f = new Foo();
    passImplicitEnclosing(f);
  }

  void passExplicitEnclosingTest() {
    // :: warning: [required.method.not.called]
    Foo f = new Foo();
    passExplicitEnclosing(this, f);
  }
}
