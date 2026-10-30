import org.checkerframework.checker.nullness.qual.Nullable;

// A comparison of an assignment refines only the sides that have the assigned value afterward.
public class AssignmentInComparison {

  @Nullable AssignmentInComparison next;

  @Nullable AssignmentInComparison other;

  static void localTarget(@Nullable AssignmentInComparison n) {
    if (n == null) {
      return;
    }
    if ((n = n.next) != null) {
      n.toString();
      // After the assignment, `n.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }

  static void localTargetNull(@Nullable AssignmentInComparison m, AssignmentInComparison n) {
    if ((m = n.next) != null) {
      m.toString();
      n.next.toString();
    }
  }

  static void fieldTarget(AssignmentInComparison n, AssignmentInComparison o) {
    if ((n.next = o.other) != null) {
      n.next.toString();
      // The assignment to `n.next` might change `o.other`, if `n` and `o` alias.
      // :: error: [dereference.of.nullable]
      o.other.toString();
    }
  }

  static void fieldTargetLocalRhs(AssignmentInComparison n, @Nullable AssignmentInComparison o) {
    if ((n.next = o) != null) {
      n.next.toString();
      o.toString();
    }
  }

  static void receiverReassigned(
      @Nullable AssignmentInComparison n, @Nullable AssignmentInComparison o) {
    if (n == null) {
      return;
    }
    if ((n.next = (n = o)) != null) {
      n.toString();
      o.toString();
      // After the assignment, `n.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }
}
