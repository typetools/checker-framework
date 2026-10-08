// A constructor may be annotated @SideEffectFree, so the obligation on an argument passed to a
// functional-interface parameter is checked at a `new` expression too.
//
// Each call site is in an unannotated method, so the only diagnostic a call site can produce is the
// one under test.

import java.util.function.Function;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityFunctionalConstructor {

  int count = 0;

  static class Holder {
    Function<String, Integer> held;

    @SideEffectFree
    Holder(Function<String, Integer> f) {
      this.held = f;
    }
  }

  static class UnannotatedHolder {
    UnannotatedHolder(Function<String, Integer> f) {}
  }

  void constructorCallSites() {
    new Holder(t -> t.length());
    // :: error: [purity.assign.field]
    new Holder(t -> count++);
    new UnannotatedHolder(t -> count++);
  }

  /** An anonymous class's arguments are passed to the super constructor. */
  void anonymousSubclass() {
    new Holder(t -> t.length()) {};
    // :: error: [purity.assign.field]
    new Holder(t -> count++) {};
  }

  static class VarargsHolder {
    // The parameter type is not generic, to avoid javac's warnings about generic varargs.
    @SideEffectFree
    VarargsHolder(Runnable... rs) {}
  }

  /**
   * The body of a varargs constructor may not assume anything about the elements of its array, so
   * no argument that a call passes to the array is checked.
   */
  void varargsConstructorCallSites() {
    new VarargsHolder(() -> {});
    new VarargsHolder(() -> count++);
    new VarargsHolder(() -> {}, () -> count++) {};
  }

  /** A functional-interface parameter may be passed onward to a constructor. */
  @SideEffectFree
  Holder passesParameterToConstructor(Function<String, Integer> f) {
    return new Holder(f);
  }
}
