import org.checkerframework.checker.index.qual.LTEqLengthOf;

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
  @LTEqLengthOf("this.a") int shrink() {
    a = new int[1];
    return 1;
  }

  // If this.a has length 2 before the call, then i is 2 but this.a has length 0 after the call.
  void eqChanged() {
    int i;
    if (this.a.length == (i = replaceAndReturnTwo())) {
      // :: error: (assignment)
      @LTEqLengthOf("this.a") int j = i;
    }
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

  void neqUnchanged(@LTEqLengthOf("this.a") int k) {
    int i;
    if (this.a.length != (i = k)) {
      int x = this.a[i];
    }
  }
}
