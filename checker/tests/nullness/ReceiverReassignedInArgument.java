// A call tests its receiver for nullness after evaluating its arguments, so the receiver is refined
// to non-null after the call.  That refinement must not happen if an argument reassigns the
// receiver expression.

import org.checkerframework.checker.nullness.qual.Nullable;

class ReceiverReassignedInArgument {

  @Nullable Object f;

  void localAssignedNull() {
    @Nullable Object x = new Object();
    x.equals(x = null);
    // :: error: (dereference.of.nullable)
    x.hashCode();
  }

  void localAssignedNullNested(@Nullable Object y) {
    @Nullable Object x = new Object();
    x.equals(String.valueOf(x = y));
    // :: error: (dereference.of.nullable)
    x.hashCode();
  }

  void localNotAssigned(@Nullable Object x, @Nullable Object y) {
    // :: error: (dereference.of.nullable)
    x.equals(y = null);
    x.hashCode();
  }

  void fieldAssignedNull() {
    this.f = new Object();
    this.f.equals(this.f = null);
    // :: error: (dereference.of.nullable)
    this.f.hashCode();
  }

  void fieldSetToNullByCall() {
    this.f = new Object();
    this.f.equals(setFNull());
    // :: error: (dereference.of.nullable)
    this.f.hashCode();
  }

  Object setFNull() {
    f = null;
    return "";
  }

  void arrayIndexIncremented(@Nullable Object[] a, int i) {
    if (a[i] != null) {
      a[i].equals(i++);
      // :: error: (dereference.of.nullable)
      a[i].hashCode();
    }
  }
}
