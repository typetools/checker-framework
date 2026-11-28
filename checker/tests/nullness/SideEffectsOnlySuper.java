// A `@SideEffectsOnly("this")` method that is called via `super` side-effects the object that the
// caller denotes as `this`, so refinements of that object's fields must be discarded.
//
// Conversely, `super` in a callee's `@SideEffectsOnly` annotation denotes the callee's receiver,
// not the caller's `this`.

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class SideEffectsOnlySuper {

  static class Super {
    @Nullable Object f;

    @SideEffectsOnly("this")
    void clear() {
      f = null;
    }
  }

  static class Sub extends Super {
    void viaSuper() {
      f = new Object();
      super.clear();
      // :: error: (dereference.of.nullable)
      f.toString();
    }

    void viaThis() {
      f = new Object();
      this.clear();
      // :: error: (dereference.of.nullable)
      f.toString();
    }
  }

  static class Box {
    @Nullable Object content;
  }

  static class Super2 {
    @Nullable Object f;
    Box box = new Box();

    @Pure
    Box get() {
      return box;
    }
  }

  static class HidingSub extends Super2 {
    // Hides `Super2.f`.
    @Nullable Object f;

    Box otherBox = new Box();

    @Override
    @Pure
    Box get() {
      return otherBox;
    }

    @SideEffectsOnly("super.f")
    void clearSuperF() {
      super.f = null;
    }

    // `super.get()` is `box`.
    @SideEffectsOnly("super.get()")
    void clearSuperGet() {
      super.get().content = null;
    }

    @SideEffectsOnly("this")
    void clearAll() {
      super.f = null;
      super.get().content = null;
    }

    void refinesSuperF() {
      super.f = new Object();
      clearAll();
      // :: error: (dereference.of.nullable)
      super.f.toString();
    }

    void refinesSuperGet() {
      super.get().content = new Object();
      clearAll();
      // :: error: (dereference.of.nullable)
      super.get().content.toString();
    }
  }

  static class Caller extends Super2 {
    // The callee side-effects `s`'s `Super2.f`, not the caller's.
    void clearsOtherSuperF(HidingSub s) {
      f = new Object();
      s.clearSuperF();
      f.toString();
    }

    // The callee side-effects `s.box.content`.
    void clearsOtherSuperGet(HidingSub s) {
      s.box.content = new Object();
      s.clearSuperGet();
      // :: error: (dereference.of.nullable)
      s.box.content.toString();
    }
  }
}
