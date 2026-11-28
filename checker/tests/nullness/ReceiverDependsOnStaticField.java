import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

// The receiver of a call is a method call whose result depends on a static field.  If an argument
// writes that static field, normal completion of the call does not show that the receiver
// expression is non-null afterward.  See also ReceiverReassignedInArgument.java.
public class ReceiverDependsOnStaticField {

  static @Nullable ReceiverDependsOnStaticField slot;

  @Pure
  static @Nullable ReceiverDependsOnStaticField current() {
    return slot;
  }

  @SideEffectFree
  void sefAccept(@Nullable Object o) {}

  void directAssignment() {
    if (current() == null) {
      return;
    }
    current().sefAccept(slot = null);
    // :: error: (dereference.of.nullable)
    current().toString();
  }

  void clearSlot() {
    slot = null;
  }

  void acceptNonNull(Object o) {}

  // The invoked method is not side-effect-free and might write the static field, so normal
  // completion of the call does not show that the receiver is non-null afterward.
  // These tests do not first test `current()` against null:  the store retains a refinement of a
  // static call with no modifiable argument, such as `current()`, across a call that is not
  // side-effect-free (see sideeffectsonly/StaticPureCallRefinement.java).
  void sideEffectingCallNoArguments() {
    // :: error: (dereference.of.nullable)
    current().clearSlot();
    // :: error: (dereference.of.nullable)
    current().toString();
  }

  // Likewise for an argument passed to a non-null parameter.
  void sideEffectingCallNonNullArgument() {
    // :: error: (argument)
    acceptNonNull(current());
    // :: error: (dereference.of.nullable)
    current().toString();
  }
}
