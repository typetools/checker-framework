import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

// A comparison of an assignment refines only the sides that have the assigned value afterward.
public class AssignmentInComparison {

  @Nullable AssignmentInComparison next;

  @Nullable AssignmentInComparison other;

  static @Nullable AssignmentInComparison global;

  @Pure
  @Nullable AssignmentInComparison getNext() {
    return next;
  }

  @Pure
  static @Nullable AssignmentInComparison id(@Nullable AssignmentInComparison n) {
    return n;
  }

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

  static void localTargetFieldRhs(@Nullable AssignmentInComparison m, AssignmentInComparison n) {
    if ((m = n.next) != null) {
      m.toString();
      n.next.toString();
    }
  }

  static void fieldTarget(AssignmentInComparison n, AssignmentInComparison o) {
    if ((n.next = o.other) != null) {
      n.next.toString();
      // The assignment to `n.next` does not change `o.other`, which is a different field.
      o.other.toString();
    }
  }

  static void fieldTargetSameField(AssignmentInComparison n, AssignmentInComparison o) {
    if ((n.next = o.next) != null) {
      n.next.toString();
      // If `n` and `o` alias, the assignment stores into `o.next` the value it already has.
      o.next.toString();
    }
  }

  static void fieldTargetChangesRhsReceiver(AssignmentInComparison n, AssignmentInComparison o) {
    if (o.next == null) {
      return;
    }
    if ((n.next = o.next.other) != null) {
      n.next.toString();
      // The assignment to `n.next` might change `o.next`, if `n` and `o` alias.
      // :: error: [dereference.of.nullable]
      // :: error: [dereference.of.nullable]
      o.next.other.toString();
    }
  }

  static void fieldTargetMethodCallRhs(AssignmentInComparison n, AssignmentInComparison o) {
    if ((n.next = o.getNext()) != null) {
      n.next.toString();
      // The assignment to `n.next` might change the result of `o.getNext()`.
      // :: error: [dereference.of.nullable]
      o.getNext().toString();
    }
  }

  static void staticFieldRhs(AssignmentInComparison n) {
    if ((n.next = global) != null) {
      n.next.toString();
      global.toString();
    }
  }

  void chainOfFieldTargets(@Nullable AssignmentInComparison o) {
    if ((this.next = this.other = o) != null) {
      this.next.toString();
      this.other.toString();
      o.toString();
    }
  }

  static void arrayTarget(@Nullable Object[] a, @Nullable Object[] b) {
    if ((a[0] = b[1]) != null) {
      a[0].toString();
      // If `a` and `b` alias, the assignment stores into `b[1]` the value it already has.
      b[1].toString();
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

  static void receiverReassignedInRhsReceiver(
      @Nullable AssignmentInComparison n, AssignmentInComparison o) {
    if (n == null) {
      return;
    }
    if ((n.next = (n = o).other) != null) {
      // After the assignment, `n.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }

  static void receiverReassignedInArgument(
      @Nullable AssignmentInComparison n, AssignmentInComparison o) {
    if (n == null) {
      return;
    }
    if ((n.next = id(n = o)) != null) {
      // After the assignment, `n.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }

  static void indexIncrementedInRhs(@Nullable Object[] a, int i, Object o) {
    if ((a[i] = id2(i++, o)) != null) {
      // After the assignment, `a[i]` is a different element of `a`.
      // :: error: [dereference.of.nullable]
      a[i].toString();
    }
  }

  @Pure
  static @Nullable Object id2(int i, @Nullable Object o) {
    return o;
  }

  static <T> void arrayTargetTypeVariable(@Nullable T[] ts, @Nullable String[][] ss) {
    if ((ts[0] = (T) ss[0][0]) != null) {
      ts[0].toString();
      // The assignment to `ts[0]` might change `ss[0]`, if `ts` and `ss` alias.
      // :: error: [dereference.of.nullable]
      ss[0][0].toString();
    }
  }
}
