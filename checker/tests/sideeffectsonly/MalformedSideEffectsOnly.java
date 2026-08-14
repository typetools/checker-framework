import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class MalformedSideEffectsOnly {

  // An unparseable @SideEffectsOnly expression is reported as an error, not a crash.
  @SideEffectsOnly("#1.noSuchMethod()")
  // :: error: (flowexpr.parse.error)
  void method(Object o) {}

  // A parse error whose message key is not `flowexpr.parse.error` is also reported as an error,
  // not a crash.
  @SideEffectsOnly("#2")
  // :: error: (flowexpr.parse.index.too.big)
  void tooFewParameters(Object o) {}

  // An expression that may denote a different value each time it is evaluated is rejected at the
  // declaration.  Two evaluations of `#1.getList()` are treated as unrelated, so nothing that the
  // body modifies would ever be recognized as the listed expression, not even the syntactically
  // identical `h.getList()`.
  @SideEffectsOnly("#1.getList()")
  // :: error: (purity.unstable.sideeffectsonly)
  void unstableExpression(Holder h) {
    h.getList().add("x");
  }

  // An array access is stable only if its index is stable too.
  @SideEffectsOnly("#1.lists[#1.size()]")
  // :: error: (purity.unstable.sideeffectsonly)
  void unstableIndex(Holder h) {}

  // A field access, an array access, and a formal parameter are all stable.
  @SideEffectsOnly("#1.lists[0]")
  void stableExpression(Holder h) {
    h.lists[0].add("x");
  }

  // A `@Pure` method returns the same value every time it is called with the same arguments, so a
  // call to one is stable when its receiver and its arguments are.
  @SideEffectsOnly("#1.getPureList()")
  void stablePureCall(Holder h) {
    h.getPureList().add("x");
  }

  // A `@Pure` call is stable enough to serve as an array index, too.
  @SideEffectsOnly("#1.lists[#1.pureSize()]")
  void stablePureIndex(Holder h) {
    h.lists[h.pureSize()].add("x");
  }

  // A `@Pure` call is satisfiable at a call site as well as in a body:  the callee's expression,
  // view-adapted to the call site, is the caller's.
  @SideEffectsOnly("#1.getPureList()")
  void callsStablePureCallee(Holder h) {
    stablePureCall(h);
  }

  @SideEffectsOnly("#1.lists[0]")
  void callsStablePureCalleeNotOk(Holder h) {
    // :: error: (purity.incorrect.sideeffectsonly)
    stablePureCall(h);
  }

  // `getPureAt` is `@Pure`, but its argument may differ between two evaluations.
  @SideEffectsOnly("#1.getPureAt(#1.size())")
  // :: error: (purity.unstable.sideeffectsonly)
  void unstablePureCallArgument(Holder h) {}

  // `@SideEffectFree` alone is not enough:  a method that is not `@Deterministic` may return a
  // different object each time it is called.
  @SideEffectsOnly("#1.getSideEffectFreeList()")
  // :: error: (purity.unstable.sideeffectsonly)
  void unstableSideEffectFreeCall(Holder h) {}

  static class Holder {
    java.util.List<String>[] lists;

    java.util.List<String> getList() {
      throw new Error("not called");
    }

    @Pure
    java.util.List<String> getPureList() {
      throw new Error("not called");
    }

    @Pure
    java.util.List<String> getPureAt(int i) {
      throw new Error("not called");
    }

    @SideEffectFree
    java.util.List<String> getSideEffectFreeList() {
      throw new Error("not called");
    }

    @Pure
    int pureSize() {
      return 0;
    }

    int size() {
      return 0;
    }
  }
}
