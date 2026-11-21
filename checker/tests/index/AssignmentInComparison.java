import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

// A comparison of an assignment refines only the sides that have the assigned value afterward.
public class AssignmentInComparison {

  @NonNegative int f;

  int[] arr = new int[0];

  // A field, so that the Less Than Checker does not refine an expression to be less than it.
  @LTLengthOf("arr") int lessThanArrLength;

  int[] setArr(int[] c) {
    arr = c;
    return c;
  }

  @NonNegative int resetArr(int[] c) {
    arr = c;
    return 0;
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

  @NonNegative int clearArr() {
    arr = new int[0];
    return 0;
  }

  // The right operand reassigns `arr` after the left operand reads `arr.length`.
  void otherOperandReassignsComparedSequence() {
    int i;
    if (arr.length > (i = clearArr())) {
      // :: error: [array.access.unsafe.high.range]
      int value = arr[i];
    }
  }

  // The right operand assigns `k`, so the comparison does not imply that `j + k < a.length` for
  // the new value of `k`.
  static void otherOperandReassignsOffset(@NonNegative int k, @NonNegative int j) {
    int[] a = new int[110];
    if (k < a.length - (j = (k = 100))) {
      // :: error: [array.access.unsafe.high.constant]
      a[j + k] = 0;
    }
  }

  // The right operand assigns `k`, so the comparison does not imply that `j + k < a.length` for
  // the new value of `k`.
  static void otherOperandReassignsAdditionOffset(@NonNegative int k, @NonNegative int n) {
    int[] a = new int[110];
    int j;
    if (k + (j = n) < (k = a.length)) {
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

  // The dimension reassigns `h`, so after the assignment `h.arr` is not the created array.
  static void arrayCreationRelocatesTarget(
      AssignmentInComparison h, AssignmentInComparison other, @NonNegative int x) {
    int i;
    h.arr = new int[(i = x) + (h = other).arr.length];
    // :: error: [assignment]
    @LTEqLengthOf("h.arr") int y = i;
  }

  static void otherOperandReassignsOffset(int[] a, int x, int y, int z) {
    if ((x = y) + z < (z = a.length)) {
      // `z` is `a.length`, not the value that was added to `x`.
      // :: error: [assignment]
      @LTLengthOf(value = "a", offset = "z") int w = x;
    }
  }

  static void offsetUnchanged(int[] a, int x, int y, int z) {
    if ((x = y) + z < a.length) {
      @LTLengthOf(value = "a", offset = "z") int w = x;
      @LTLengthOf(value = "a", offset = "z") int v = y;
    }
  }

  @SideEffectFree
  static int choose() {
    return (int) System.nanoTime();
  }

  @Pure
  static int deterministic() {
    return 1;
  }

  static void nondeterministicOffset(int[] a, int x, int y) {
    if ((x = y) + choose() < a.length) {
      // Another call to `choose()` might return a different value than the one added to `x`.
      @LTLengthOf(value = "a", offset = "choose()")
      // :: error: [assignment]
      int w = x;
    }
  }

  static void deterministicOffset(int[] a, int x, int y) {
    if ((x = y) + deterministic() < a.length) {
      @LTLengthOf(value = "a", offset = "deterministic()")
      int w = x;
    }
  }

  void otherOperandReassignsLengthReceiver(int[] b, int[] c) {
    if ((this.arr = b).length == setArr(c).length) {
      // `setArr` sets `this.arr` to `c`, which might not have the same length as `b`.
      // :: error: [assignment]
      int @SameLen("b") [] x = this.arr;
    }
  }

  void otherOperandReassignsArrayOfGreater(int i, int[] c) {
    if (lessThanArrLength > (i = (arr = c).length)) {
      // `lessThanArrLength` is less than the length of the array that `arr` referred to before the
      // right operand reassigned `arr`.  `i` is the length of the new array.
      // :: error: [assignment]
      @LTLengthOf("arr") int y = i;
    }
  }

  void otherOperandReassignsArrayOfGreaterOrEqual(int i, int[] c) {
    if (lessThanArrLength >= (i = (arr = c).length)) {
      // :: error: [assignment]
      @LTLengthOf("arr") int y = i;
    }
  }

  void otherOperandCallReassignsArray(@LTLengthOf("arr") int x, @NonNegative int i, int[] c) {
    if (x > i + resetArr(c)) {
      // `resetArr` reassigns `arr`, so `i` might not be less than the length of the new array.
      // :: error: [assignment]
      @LTLengthOf("arr") int y = i;
    }
  }
}
