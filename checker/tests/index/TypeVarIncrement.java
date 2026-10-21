// Test case for increments and decrements of a type variable bounded by a primitive wrapper.
//
// A caller may instantiate a type variable with any qualifier between its bounds.  An increment
// can make a value violate an upper-bound qualifier, and a decrement can make a value violate a
// lower-bound qualifier, so those operations on a value whose type is a type variable are errors.

import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class TypeVarIncrement {
  <T extends Integer> void increment(T t) {
    // :: error: [unary.increment]
    ++t;
  }

  <T extends Integer> void decrement(T t) {
    // :: error: [unary.decrement]
    --t;
  }

  static <T extends Integer> T decrementAndReturn(T t) {
    // :: error: [unary.decrement]
    --t;
    return t;
  }

  static void useDecrementAndReturn() {
    @NonNegative Integer zero = 0;
    // If decrementAndReturn type-checked, this would be -1.
    @NonNegative Integer minusOne = decrementAndReturn(zero);
  }

  <T extends @Positive Integer> void incrementPositive(T t) {
    // Only the upper bound may be violated.
    // :: error: [unary.increment]
    ++t;
  }

  <T extends @NonNegative Integer> void decrementNonNegative(T t) {
    // :: error: [unary.decrement]
    --t;
  }

  <T extends @LTLengthOf("#2") Integer> void decrementLTLengthOf(T t, int[] a) {
    // Only the lower bound may be violated.
    // :: error: [unary.decrement]
    --t;
  }
}
