// Test case for https://github.com/typetools/checker-framework/issues/2452

import java.lang.reflect.Array;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.common.value.qual.MinLen;

class Issue2452 {
  Object m1(Object[] a1) {
    if (Array.getLength(a1) > 0) {
      return Array.get(a1, 0);
    } else {
      return null;
    }
  }

  void m2() {
    int[] arr = {1, 2, 3};
    @LTEqLengthOf({"arr"}) int a = Array.getLength(arr);
  }

  void testMinLenSubtractPositive(String @MinLen(10) [] s) {
    @Positive int i1 = s.length - 9;
    @NonNegative int i0 = Array.getLength(s) - 10;
    // ::  error: (assignment.type.incompatible)
    @NonNegative int im1 = Array.getLength(s) - 11;
  }

  void testLessThanLength(String[] s, @IndexOrHigh("#1") int i, @IndexOrHigh("#1") int j) {
    if (i < Array.getLength(s)) {
      @IndexFor("s") int in = i;
      // ::  error: (assignment.type.incompatible)
      @IndexFor("s") int jn = j;
    }
  }
}
