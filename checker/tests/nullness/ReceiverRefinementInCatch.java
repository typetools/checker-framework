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

  void read() throws java.io.IOException {}

  // A null receiver causes a NullPointerException, which a handler for IOException does not catch.
  // So the receiver is non-null in that handler.
  void checkedExceptionHandler(@Nullable ReceiverRefinementInCatch r) {
    try {
      // :: error: (dereference.of.nullable)
      r.read();
    } catch (java.io.IOException e) {
      r.toString();
    }
  }

  void checkedAndRuntimeExceptionHandlers(@Nullable ReceiverRefinementInCatch r) {
    try {
      // :: error: (dereference.of.nullable)
      r.read();
    } catch (java.io.IOException e) {
      r.toString();
    } catch (RuntimeException e) {
      // :: error: (dereference.of.nullable)
      r.toString();
    }
  }

  // The invocation's RuntimeException edge reaches a handler for a narrower runtime exception, but
  // that edge represents an exception thrown by read(), which ran only because r is non-null.
  void narrowerRuntimeExceptionHandler(@Nullable ReceiverRefinementInCatch r) {
    try {
      // :: error: (dereference.of.nullable)
      r.read();
    } catch (IllegalArgumentException e) {
      r.toString();
    } catch (java.io.IOException e) {
      r.toString();
    }
  }

  // A null iterable causes a NullPointerException before iterator() runs.
  void enhancedForNullIterable(java.util.@Nullable List<Object> list) {
    try {
      // :: error: (iterating.over.nullable)
      for (Object o : list) {}
    } catch (NullPointerException e) {
      // :: error: (dereference.of.nullable)
      list.size();
    }
  }

  void throwNull() {
    try {
      // :: error: (throwing.nullable)
      throw null;
    } catch (NullPointerException e) {
    }
  }

  // An Error handler cannot catch the NullPointerException either.
  void errorHandler(@Nullable Object o) {
    try {
      // :: error: (dereference.of.nullable)
      o.toString();
    } catch (Error e) {
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

  // The argument increments the index of the receiver, so afterward a[i] denotes a different
  // array element.
  void argumentIncrementsReceiverIndex(@Nullable ReceiverRefinementInCatch[] a, int i) {
    if (a[i] == null) {
      return;
    }
    a[i].sefAccept(i++);
    // :: error: (dereference.of.nullable)
    a[i].toString();
  }

  void argumentDecrementsReceiverIndex(@Nullable ReceiverRefinementInCatch[] a, int i) {
    if (a[i] == null) {
      return;
    }
    a[i].sefAccept(--i);
    // :: error: (dereference.of.nullable)
    a[i].toString();
  }

  void argumentDoesNotReassignReceiver(@Nullable Object o, @Nullable Object other) {
    // :: error: (dereference.of.nullable)
    o.equals(other = null);
    o.hashCode();
  }
}
