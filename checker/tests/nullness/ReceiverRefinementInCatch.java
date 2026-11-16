import org.checkerframework.checker.nullness.qual.Nullable;

// A call's NullPointerException for a null receiver must not carry a non-null refinement of the
// receiver into a catch block.
public class ReceiverRefinementInCatch {

  void m(@Nullable Object o) {
    try {
      // :: error: (dereference.of.nullable)
      o.toString();
    } catch (NullPointerException e) {
      // :: error: (dereference.of.nullable)
      o.hashCode();
    }
  }

  void afterNormalCompletion(@Nullable Object o) {
    // :: error: (dereference.of.nullable)
    o.toString();
    // The call completed normally, so o is non-null.
    o.hashCode();
  }

  void accept(@Nullable Object o) {}

  // The receiver is evaluated before the argument, so the call succeeds, but afterward r is null.
  void argumentReassignsReceiver(@Nullable ReceiverRefinementInCatch r) {
    if (r == null) {
      return;
    }
    r.accept(r = null);
    // :: error: (dereference.of.nullable)
    r.toString();
  }

  @Nullable ReceiverRefinementInCatch f;

  void clearF() {
    f = null;
  }

  @org.checkerframework.dataflow.qual.SideEffectFree
  void sefAccept(@Nullable Object o) {}

  void argumentReassignsFieldReceiver() {
    // :: error: (dereference.of.nullable)
    f.sefAccept(f = null);
    // :: error: (dereference.of.nullable)
    f.toString();
  }

  void argumentSideEffectOnFieldReceiver() {
    // :: error: (dereference.of.nullable)
    f.sefAccept(clearFAndReturn());
    // :: error: (dereference.of.nullable)
    f.toString();
  }

  @Nullable Object clearFAndReturn() {
    clearF();
    return null;
  }

  void argumentDoesNotReassignReceiver(@Nullable Object o, @Nullable Object other) {
    // :: error: (dereference.of.nullable)
    o.equals(other = null);
    o.hashCode();
  }
}
