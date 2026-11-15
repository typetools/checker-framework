import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.dataflow.qual.Pure;

// A comparison of an assignment refines only the sides that have the assigned value afterward.
public class AssignmentInComparison {

  @NonNegative int f;

  int[] arr = new int[0];

  int[] setArr(int[] c) {
    arr = c;
    return c;
  }

  @Pure
  @NonNegative int size() {
    return f;
  }

  static void localTarget(@NonNegative int i, @NonNegative int j) {
    if ((i = j) > 0) {
      @Positive int x = i;
      @Positive int y = j;
    }
  }

  static void localTargetMethodCallRhs(@NonNegative int i, AssignmentInComparison n) {
    if ((i = n.size()) > 0) {
      @Positive int x = i;
      @Positive int y = n.size();
    }
  }

  // The right operand assigns `i` after the left operand reads it, so the comparison says nothing
  // about the new value of `i`.
  static void assignmentInRightOperand(@NonNegative int i) {
    if (i > (i = 0)) {
      // :: error: [assignment]
      @Positive int x = i;
    }
  }

  // The right operand assigns `i` after the left operand reads it, so the comparison does not
  // imply that the new value of `i` is less than `i`.
  static void assignmentInRightOperandLessThan(@NonNegative int i) {
    if (i > (i = 0)) {
      // :: error: [assignment]
      @Positive int z = i - i;
    }
  }

  static void assignmentInRightOperandLessThanGTE(@NonNegative int i) {
    if (i - 1 >= (i = 0)) {
      // :: error: [assignment]
      @LessThan("i") int z = i;
    }
  }

  static void fieldTargetMethodCallRhs(AssignmentInComparison n) {
    if ((n.f = n.size()) > 0) {
      @Positive int x = n.f;
      // The assignment to `n.f` might change the result of `n.size()`.
      // :: error: [assignment]
      @Positive int y = n.size();
    }
  }

  static void otherOperandReassignsLeftOperand(@NonNegative int i) {
    if (i > (i = 0)) {
      // `i` is 0, not the value that was compared.
      // :: error: [assignment]
      @Positive int x = i;
    }
  }

  // The right operand assigns `k`, so the comparison does not imply that `j + k < a.length` for
  // the new value of `k`.
  static void otherOperandReassignsOffset(@NonNegative int k, @NonNegative int j) {
    int[] a = new int[110];
    if (k < a.length - (j = (k = 100))) {
      // :: error: [array.access.unsafe.high]
      a[j + k] = 0;
    }
  }

  static void targetSameLen(AssignmentInComparison n, int[] a, int @SameLen("#2") [] b) {
    n.arr = b;
    int @SameLen("a") [] x = n.arr;
  }

  // The assignment writes the `arr` field of the original `n`, not of `other`.
  static void receiverReassignedSameLen(
      AssignmentInComparison n, AssignmentInComparison other, int[] a, int @SameLen("#3") [] b) {
    n.arr = (n = other) == null ? b : b;
    // :: error: [assignment]
    int @SameLen("a") [] x = n.arr;
    int @SameLen("a") [] y = b;
  }

  void otherOperandReassignsLengthReceiver(int[] b, int[] c) {
    if ((this.arr = b).length == setArr(c).length) {
      // `setArr` sets `this.arr` to `c`, which might not have the same length as `b`.
      // :: error: [assignment]
      int @SameLen("b") [] x = this.arr;
    }
  }
}
