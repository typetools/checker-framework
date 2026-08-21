import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class MalformedSideEffectsOnly {

  // An unparseable @SideEffectsOnly expression is reported as an error, not a crash.  Two checks
  // detect it -- checking the annotation itself, and parsing the expression in order to check the
  // method body against it -- but they issue the same message, so it is reported only once.
  @SideEffectsOnly("#1.noSuchMethod()")
  // :: error: (flowexpr.parse.error.sideeffectsonly)
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
  // :: error: (purity.nondeterministic.sideeffectsonly)
  void nondeterministicExpression(Holder h) {
    h.getList().add("x");
  }

  // An array access is deterministic only if its index is deterministic too.
  @SideEffectsOnly("#1.lists[#1.size()]")
  // :: error: (purity.nondeterministic.sideeffectsonly)
  void nondeterministicIndex(Holder h) {}

  // A field access, an array access, and a formal parameter are all deterministic.
  @SideEffectsOnly("#1.lists[0]")
  void deterministicExpression(Holder h) {
    h.lists[0].add("x");
  }

  // A `@Pure` method returns the same value every time it is called with the same arguments, so a
  // call to one is deterministic when its receiver and its arguments are.
  @SideEffectsOnly("#1.getPureList()")
  void deterministicPureCall(Holder h) {
    h.getPureList().add("x");
  }

  // A `@Pure` call is deterministic enough to serve as an array index, too.
  @SideEffectsOnly("#1.lists[#1.pureSize()]")
  void deterministicPureIndex(Holder h) {
    h.lists[h.pureSize()].add("x");
  }

  // A `@Pure` call is satisfiable at a call site as well as in a body:  the callee's expression,
  // view-adapted to the call site, is the caller's.
  @SideEffectsOnly("#1.getPureList()")
  void callsDeterministicPureCallee(Holder h) {
    deterministicPureCall(h);
  }

  @SideEffectsOnly("#1.lists[0]")
  void callsDeterministicPureCalleeNotOk(Holder h) {
    // :: error: (purity.incorrect.sideeffectsonly)
    deterministicPureCall(h);
  }

  // `getPureAt` is `@Pure`, but its argument may differ between two evaluations.
  @SideEffectsOnly("#1.getPureAt(#1.size())")
  // :: error: (purity.nondeterministic.sideeffectsonly)
  void nondeterministicPureCallArgument(Holder h) {}

  // `@SideEffectFree` alone is not enough:  a method that is not `@Deterministic` may return a
  // different object each time it is called.
  @SideEffectsOnly("#1.getSideEffectFreeList()")
  // :: error: (purity.nondeterministic.sideeffectsonly)
  void nondeterministicSideEffectFreeCall(Holder h) {}

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
