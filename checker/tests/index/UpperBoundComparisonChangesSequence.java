import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;

// A bound in the type of an operand of a comparison refers to a sequence as it was when the operand
// was evaluated.  If the other operand reassigns the sequence, the bound must not be used to refine
// the types of expressions after the comparison.

public class UpperBoundComparisonChangesSequence {

  int[] a = new int[2];

  // Not side-effect-free.
  int replaceAndReturnTwo() {
    a = new int[0];
    return 2;
  }

  // Not side-effect-free.
  @NonNegative @LTEqLengthOf("this.a") int shrink() {
    a = new int[1];
    return 1;
  }

  void eqUnchanged(int k) {
    int i;
    if (this.a.length == (i = k)) {
      @LTEqLengthOf("this.a") int j = i;
    }
  }

  // If this.a has length 2 before the call, then the comparison is 2 != 1, but i is 1 and this.a
  // has length 1 after the call.
  void neqChanged() {
    int i;
    if (this.a.length != (i = shrink())) {
      // :: error: (array.access.unsafe.high)
      int x = this.a[i];
    }
  }

  void neqUnchanged(@NonNegative @LTEqLengthOf("this.a") int k) {
    int i;
    if (this.a.length != (i = k)) {
      int x = this.a[i];
    }
  }

  // The left operand is an assignment whose value, `this.a.length`, does not change.
  void gtAssignmentOperand(@NonNegative int k) {
    int limit;
    if ((limit = this.a.length) > k) {
      int x = this.a[k];
    }
  }

  void gteAssignmentOperand(@NonNegative int k) {
    int limit;
    if ((limit = this.a.length) >= k) {
      @LTEqLengthOf("this.a") int j = k;
    }
  }

  // The right operand reassigns `this.a`, so the bound from the left operand does not hold.
  void gtAssignmentOperandChanged(@NonNegative int k) {
    int limit;
    if ((limit = this.a.length) > k + replaceAndReturnTwo()) {
      // :: error: (array.access.unsafe.high)
      int x = this.a[k];
    }
  }

  // If a has length 10 and b has length 1, then the comparison is 10 > 1, but x is 1 and a has
  // length 1 after the comparison.
  void gtChanged(int[] a, int[] b) {
    int x;
    if (a.length > (x = (a = b).length)) {
      // :: error: (array.access.unsafe.high.range)
      int y = a[x];
    }
  }

  void gtUnchanged(int[] a, int[] b) {
    int x;
    if (a.length > (x = b.length)) {
      int y = a[x];
    }
  }

  // If a has length 10 and b has length 1, then the comparison is 10 >= 1, but x is 1 and a has
  // length 1 after the comparison.
  void gteChanged(int[] a, int[] b) {
    int x;
    if (a.length >= (x = (a = b).length + 1)) {
      // :: error: (assignment)
      @LTEqLengthOf("a") int y = x;
    }
  }

  void gteUnchanged(int[] a, int[] b) {
    int x;
    if (a.length >= (x = b.length + 1)) {
      @LTEqLengthOf("a") int y = x;
    }
  }
}
