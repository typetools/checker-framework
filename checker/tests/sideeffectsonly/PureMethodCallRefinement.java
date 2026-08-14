// A `@SideEffectsOnly` annotation says what the callee writes, but says nothing about what a
// `@Pure` method reads.  A refinement of a `@Pure` method call must therefore be discarded when
// a listed expression is reached through the call's receiver or one of its arguments, even though
// the call is not built out of the listed expression.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.qual.EnsuresQualifier;

public class PureMethodCallRefinement {

  @Tainted Object f;

  @Pure
  Object getF() {
    return f;
  }

  @EnsuresQualifier(expression = "#1.getF()", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  void makeUntainted(PureMethodCallRefinement o) {}

  @SideEffectsOnly("#1.f")
  void modifyField(PureMethodCallRefinement o) {}

  @SideEffectsOnly("this")
  void modifyThis() {}

  void test(PureMethodCallRefinement o) {
    makeUntainted(o);
    // `modifyField` may write `o.f`, which `getF()` returns.  The annotation does not mention
    // `o.getF()`, but that expression is not a subexpression of `o.f`, so a rule based only on
    // subexpressions would wrongly retain the refinement.
    modifyField(o);
    // :: error: assignment
    @Untainted Object y = o.getF();
  }

  void testNestedCall(PureMethodCallRefinement o) {
    makeUntaintedNested(o);
    // The stored expression's receiver is itself a call, so the search for the modified location
    // must recur through it.
    modifyField(o);
    // :: error: assignment
    @Untainted Object y = o.getSelf().getF();
  }

  @Pure
  PureMethodCallRefinement getSelf() {
    return this;
  }

  @EnsuresQualifier(expression = "#1.getSelf().getF()", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  void makeUntaintedNested(PureMethodCallRefinement o) {}

  void testUnrelatedReceiver(PureMethodCallRefinement o) {
    makeUntainted(o);
    // `modifyThis` modifies `this`, which is unrelated to `o` and to `o`'s fields.  Like every
    // other use of `@SideEffectsOnly` for type refinement, this is unsound if `this` and `o` are
    // aliases, or if `o` is reachable from `this`.
    modifyThis();
    @Untainted Object y = o.getF();
  }
}
