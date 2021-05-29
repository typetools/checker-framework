// Test case for Issue 167:
// https://github.com/kelloggm/checker-framework/issues/167

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LTOMLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;

public class Index167 {
  static void fn1(int[] arr, @IndexFor("#1") int i) {
    if (i >= 33) {
      // :: error: (argument)
      fn2(arr, i);
    }
    if (i > 33) {
      // :: error: (argument)
      fn2(arr, i);
    }
    if (i != 33) {
      // :: error: (argument)
      fn2(arr, i);
    }
  }

  static void fn2(int[] arr, @NonNegative @LTOMLengthOf("#1") int i) {
    int c = arr[i + 1];
  }
}
