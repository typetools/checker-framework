import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

// A comparison of an assignment refines only the sides that have the assigned value afterward.
public class AssignmentInComparison {

  @NonNegative int f;

  int[] arr = new int[0];

  int[] setArr(int[] c, int[] d) {
    arr = c;
    return d;
  }

  // Assigning the result to `f` can change the result of the next call.
  @Pure
  @NonNegative int size() {
    return f == 0 ? 1 : 0;
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

  void otherOperandReassignsLengthReceiver(int[] b, int[] c, int[] d) {
    if ((this.arr = b).length == setArr(c, d).length) {
      // `setArr` sets `this.arr` to `c`, which might not have the same length as `b` and `d`.
      // :: error: [assignment]
      int @SameLen("b") [] x = this.arr;
    }
  }
}
