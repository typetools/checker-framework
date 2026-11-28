import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

// Dereferencing an assignment or a cast refines the expressions that have the same value.
public class DereferenceAssignmentAndCast {

  String f;

  @Nullable DereferenceAssignmentAndCast next;

  DereferenceAssignmentAndCast(String f) {
    this.f = f;
  }

  @NonNull String methodAccessOfAssignment(@Nullable String s, @Nullable String t) {
    // :: error: [dereference.of.nullable]
    (s = t).length();
    s.toString();
    return t;
  }

  @NonNull Object methodAccessOfCast(@Nullable Object o) {
    // :: error: [dereference.of.nullable]
    ((String) o).length();
    return o;
  }

  @NonNull Object fieldAccessOfAssignmentOfCast(
      @Nullable DereferenceAssignmentAndCast d, @Nullable Object o) {
    // :: error: [dereference.of.nullable]
    (d = (DereferenceAssignmentAndCast) o).f.toString();
    d.toString();
    return o;
  }

  @NonNull Object arrayAccessOfCast(@Nullable Object o) {
    // :: error: [accessing.nullable]
    ((String[]) o)[0].toString();
    return o;
  }

  static void selfReferentialAssignment(@Nullable DereferenceAssignmentAndCast n) {
    if (n == null) {
      return;
    }
    // :: error: [dereference.of.nullable]
    (n = n.next).toString();
    n.toString();
    // After the assignment, `n.next` is a field of a different object.
    // :: error: [dereference.of.nullable]
    n.next.toString();
  }

  @Pure
  static int consume(@NonNull Object o, @Nullable Object p) {
    return 0;
  }

  void argumentOfAssignment(@Nullable Object x, @Nullable Object y) {
    // :: error: [argument]
    consume(x = y, null);
    x.toString();
    y.toString();
  }

  // A later argument reassigns `y`, so `y` is not non-null after the call.
  void laterArgumentAssigns(@Nullable Object x, @Nullable Object y) {
    // :: error: [argument]
    consume(x = y, y = null);
    x.toString();
    // :: error: [dereference.of.nullable]
    y.toString();
  }

  // The index reassigns `b`, so `b` is not non-null after the array access.
  void arrayAccessIndexAssigns(String @Nullable [] a, String @Nullable [] b) {
    // :: error: [accessing.nullable]
    (a = b)[(b = null) == null ? 0 : 1].toString();
    a.toString();
    // :: error: [dereference.of.nullable]
    b.toString();
  }

  void throwOfAssignment(@Nullable RuntimeException e, @Nullable RuntimeException f) {
    try {
      // :: error: [throwing.nullable]
      throw e = f;
    } finally {
      // e and f may be null if the throw itself threw NullPointerException.
      // :: error: [dereference.of.nullable]
      e.toString();
    }
  }
}
