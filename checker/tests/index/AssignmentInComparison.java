import org.checkerframework.checker.index.qual.LTLengthOf;
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

  void otherOperandReassignsLengthReceiver(int[] b, int[] c) {
    if ((this.arr = b).length == setArr(c).length) {
      // `setArr` sets `this.arr` to `c`, which might not have the same length as `b`.
      // :: error: [assignment]
      int @SameLen("b") [] x = this.arr;
    }
  }
}
