// `super` and `this` denote the same object, so an expression written with either as the receiver
// is covered by `@SideEffectsOnly("this")`.

import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class SuperSeonly {

  static class Super {
    Object f;
    int g;

    @SideEffectsOnly("this")
    void modifiesThis() {
      f = null;
    }
  }

  static class Sub extends Super {

    // Calling a `@SideEffectsOnly("this")` method through `super` side-effects the object that the
    // caller denotes as `this`.
    @SideEffectsOnly("this")
    void callsSuperMethod() {
      super.modifiesThis();
    }

    @SideEffectsOnly("this")
    void assignsThroughSuper() {
      super.f = null;
      super.g++;
    }

    @SideEffectsOnly("#1")
    void thisIsNotListed(Object unused) {
      // :: error: (purity.incorrect.sideeffectsonly)
      super.modifiesThis();
      // :: error: (purity.incorrect.sideeffectsonly)
      super.f = null;
    }
  }
}
