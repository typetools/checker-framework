// The value of a `@Pure` method call is approximated by the state reachable from the call's
// receiver and arguments.  A static call has no receiver, so state that it reads through a class
// name is invisible to that approximation.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectsOnly;
import org.checkerframework.framework.qual.EnsuresQualifier;

class StaticOther {
  static @Tainted Object field;
}

class StaticUtil {
  @Pure
  static Object get() {
    return StaticOther.field;
  }

  @Pure
  static Object getFrom(StaticPureCallRefinement o) {
    return o.f;
  }
}

public class StaticPureCallRefinement {

  @Tainted Object f;

  @EnsuresQualifier(expression = "StaticUtil.get()", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  void makeUntaintedStatic() {}

  @EnsuresQualifier(expression = "StaticUtil.getFrom(#1)", qualifier = Untainted.class)
  // :: error: contracts.postcondition
  void makeUntaintedStaticArg(StaticPureCallRefinement o) {}

  @SideEffectsOnly("StaticOther.field")
  void modifyStaticField() {}

  @SideEffectsOnly("#1.f")
  void modifyField(StaticPureCallRefinement o) {}

  void arbitrary() {}

  void testStaticReceiver() {
    makeUntaintedStatic();
    // `modifyStaticField` may write `StaticOther.field`, which `StaticUtil.get()` returns, so the
    // refinement of `StaticUtil.get()` is stale.  It is nonetheless retained, because
    // `StaticUtil.get()` is not modifiable by other code: its receiver is a class name and it has
    // no arguments, so no location that a caller could write appears in it.
    modifyStaticField();
    @Untainted Object y = StaticUtil.get();
  }

  void testStaticReceiverImpureCall() {
    makeUntaintedStatic();
    // The retention above is not specific to `@SideEffectsOnly`: the refinement of a static call
    // with no modifiable argument survives an arbitrary impure call too.
    arbitrary();
    @Untainted Object y = StaticUtil.get();
  }

  void testStaticArgument(StaticPureCallRefinement o) {
    makeUntaintedStaticArg(o);
    // By contrast, a static call whose argument reaches the modified location does lose its
    // refinement, because arguments are part of the approximation.
    modifyField(o);
    // :: error: assignment
    @Untainted Object y = StaticUtil.getFrom(o);
  }
}
