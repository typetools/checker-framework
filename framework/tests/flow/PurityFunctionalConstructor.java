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
    // :: error: [purity.not.sideeffectfree.assign.field]
    new Holder(t -> count++);
    new UnannotatedHolder(t -> count++);
  }

  /** An anonymous class's arguments are passed to the super constructor. */
  void anonymousSubclass() {
    new Holder(t -> t.length()) {};
    // :: error: [purity.not.sideeffectfree.assign.field]
    new Holder(t -> count++) {};
  }

  static class VarargsHolder {
    // The parameter type is not generic, to avoid javac's warnings about generic varargs.
    @SideEffectFree
    VarargsHolder(Runnable... rs) {}
  }

  /** A varargs constructor checks each argument that the call passes to its array. */
  void varargsConstructorCallSites() {
    new VarargsHolder(() -> {});
    // :: error: [purity.not.sideeffectfree.assign.field]
    new VarargsHolder(() -> count++);
    // :: error: [purity.not.sideeffectfree.assign.field]
    new VarargsHolder(() -> {}, () -> count++) {};
  }

  /** A functional-interface parameter may be passed onward to a constructor. */
  @SideEffectFree
  Holder passesParameterToConstructor(Function<String, Integer> f) {
    return new Holder(f);
  }
}
