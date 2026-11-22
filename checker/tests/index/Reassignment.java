// Test case for https://github.com/typetools/checker-framework/issues/8621 .
// Reassigning a variable does not invalidate a type that names it.  After the reassignment, such a
// type would describe the new value of the variable, not the value for which the type was
// established.

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.SameLen;

public class Reassignment {

  int[] f;

  // The refined type of `i` names `arr`.
  void test(int[] arr, int i) {
    if (i > 0 && i < arr.length) {
      arr = new int[0];
      // :: error: [array.access.unsafe.high]
      int j = arr[i];
    }
  }

  // The refined type of `i` names `a`.
  void refinedLocal(int[] a, int[] c) {
    int i = a.length - 1;
    if (i < 0) {
      return;
    }
    a = c;
    // :: error: [array.access.unsafe.high]
    a[i] = 0;
  }

  // The declared type of the parameter `i` names the parameter `a`.
  void declaredParameterNamesParameter(@IndexFor("#2") int i, int[] a, int[] c) {
    a = c;
    // :: error: [array.access.unsafe.high]
    a[i] = 0;
  }

  // The declared type of the parameter `i` names the field `f`.
  void declaredParameterNamesField(@IndexFor("f") int i, int[] c) {
    f = c;
    // :: error: [array.access.unsafe.high]
    f[i] = 0;
  }

  // The declared type of the local variable `i` names the local variable `a`.
  void declaredLocal(int[] a, int[] c) {
    if (a.length == 0) {
      return;
    }
    @IndexFor("a") int i = a.length - 1;
    a = c;
    // :: error: [array.access.unsafe.high]
    a[i] = 0;
  }

  // The offset in the declared type of `i` names the parameter `k`.
  void declaredOffset(
      @NonNegative @LTLengthOf(value = "#2", offset = "#3") int i, int[] a, @NonNegative int k) {
    k = 100;
    // :: error: [array.access.unsafe.high]
    a[i + k] = 0;
  }

  // The Less Than Checker refines `i` to `@LessThan("limit")`, and the declared type of `limit`
  // names `a`, which `a = c` reassigned before `limit` was compared to the length of the new array.
  void declaredLimitThroughLessThan(@LTEqLengthOf("#2") int limit, int[] a, int[] c) {
    a = c;
    int i = a.length;
    if (limit > i) {
      // :: error: [array.access.unsafe.high]
      a[i] = 0;
    }
  }

  // The declared type of `b` names `a`.
  void declaredSameLen(int[] a, int @SameLen("#1") [] b, int[] c) {
    a = c;
    // :: error: [assignment]
    int @SameLen("a") [] x = b;
  }

  // The declared type of `i` names `j`.
  void declaredLessThan(@LessThan("#2") int i, int j) {
    j = 0;
    // :: error: [assignment]
    @LessThan("j") int k = i;
  }
}
