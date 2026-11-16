import org.checkerframework.checker.nullness.qual.Nullable;

// A call's receiver is evaluated before its arguments.  If an argument reassigns the receiver
// expression, then normal completion of the call does not show that the receiver expression is
// non-null afterward.
public class ReceiverReassignedInArgument {

  void take(@Nullable Object o) {}

  void assignInArgument() {
    // A local variable's declared type is @Nullable, so it may be assigned null.
    Object r = new Object();
    r.equals(r = null);
    // :: error: (dereference.of.nullable)
    r.toString();
  }

  void assignInNestedArgument() {
    ReceiverReassignedInArgument r = new ReceiverReassignedInArgument();
    r.take(String.valueOf(r = null));
    // :: error: (dereference.of.nullable)
    r.toString();
  }

  void assignNonNullInArgument(@Nullable Object r, Object nn) {
    // :: error: (dereference.of.nullable)
    r.equals(r = nn);
    r.toString();
  }

  void noAssignment(@Nullable Object r) {
    // :: error: (dereference.of.nullable)
    r.equals(null);
    r.toString();
  }
}
