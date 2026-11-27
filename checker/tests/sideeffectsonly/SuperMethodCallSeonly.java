// `super.m()` invokes the superclass's `m` directly, whereas `this.m()` dispatches on the run-time
// class of the receiver, which might override `m`.  So the two calls can return different objects,
// and a `@SideEffectsOnly` annotation that lists one of them does not cover the other.

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class SuperMethodCallSeonly {

  static class Box {
    Object content;
  }

  static class Super {
    Box box1 = new Box();
    Object f;

    @Pure
    Box get() {
      return box1;
    }

    @SideEffectsOnly("this.get()")
    void callsGet() {
      get().content = null;
    }
  }

  static class Sub extends Super {
    Box box2 = new Box();

    // Hides `Super.f`, so that `super.f` is not parsed as `this.f`.
    Object f;

    @Override
    @Pure
    Box get() {
      return box2;
    }

    // `super.get()` is `box1`, but `this.get()` is `box2`.
    @SideEffectsOnly("this.get()")
    void modifiesSuperGet() {
      // :: error: (purity.incorrect.sideeffectsonly)
      super.get().content = null;
    }

    @SideEffectsOnly("super.get()")
    void modifiesThisGet() {
      // :: error: (purity.incorrect.sideeffectsonly)
      this.get().content = null;
    }

    @SideEffectsOnly("super.get()")
    void modifiesSuperGetAsListed() {
      super.get().content = null;
    }

    // `super` is the same object as `this`, so what is reached through `super.get()` is covered by
    // `this`.
    @SideEffectsOnly("this")
    void modifiesSuperGetThis() {
      super.get().content = null;
    }

    // No expression at a call site denotes the callee's `super.get()`.
    @SideEffectsOnly("super.get()")
    void annotatedWithSuperGet() {
      super.get().content = null;
    }

    @SideEffectsOnly("super.f")
    void modifiesSuperField() {
      super.f = null;
    }

    // At a call of the form `super.m()`, the callee's `this.get()` dispatches virtually, so it is
    // the caller's `this.get()`, not the caller's `super.get()`.
    @SideEffectsOnly("super.get()")
    void callsThroughSuper() {
      // :: error: (purity.incorrect.sideeffectsonly)
      super.callsGet();
    }
  }

  static class Caller {
    @SideEffectsOnly("#1")
    void callsSuperGetCallee(Sub s) {
      // :: error: (purity.unknown.sideeffectsonly)
      s.annotatedWithSuperGet();
    }

    // The callee's `super.f` is `Super.f` of the callee's receiver, `s`.
    @SideEffectsOnly("#1")
    void callsSuperFieldCallee(Sub s) {
      s.modifiesSuperField();
    }

    // The callee's `super.f` is `Super.f` of the callee's receiver, `s`, not of the caller's
    // `this`.
    @SideEffectsOnly("this")
    void callsSuperFieldCalleeThisListed(Sub s) {
      // :: error: (purity.incorrect.sideeffectsonly)
      s.modifiesSuperField();
    }
  }
}
