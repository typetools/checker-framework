import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.common.value.qual.IntVal;

// A bound in the type of an operand of a comparison refers to a sequence as it was when the operand
// was evaluated.  If the other operand reassigns the sequence, the bound must not be used to refine
// the types of expressions after the comparison.

public class UpperBoundComparisonChangesSequence {

  int[] a = new int[2];

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

  // Not side-effect-free.  Returns 0, so `this.a.length - reset()` has a known offset.
  @IntVal(0) int reset() {
    a = new int[1];
    return 0;
  }

  // If this.a has length 2 before the call, then the comparison is 2 != 1, but i is 1 and this.a
  // has length 1 after the call.
  void neqChangedByOffset() {
    int i;
    if (this.a.length - reset() != (i = this.a.length)) {
      // :: error: (array.access.unsafe.high)
      int x = this.a[i];
    }
  }

  @NonNegative int keepSecond(Object o, @NonNegative int x) {
    return x;
  }

  // If i is 0 and this.a has length 1 before the comparison, the comparison is 0 == 0, but this.a
  // is empty after it.
  void postfixChanged(@NonNegative @LTLengthOf("this.a") int i) {
    int j;
    if (i++ == (j = keepSecond(this.a = new int[0], 0))) {
      // :: error: (array.access.unsafe.high)
      int x = this.a[j];
    }
  }

  void postfixUnchanged(@NonNegative @LTLengthOf("this.a") int i, @NonNegative int k) {
    int j;
    if (i++ == (j = k)) {
      int x = this.a[j];
    }
  }
}
