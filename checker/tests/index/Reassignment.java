// Test case for https://github.com/typetools/checker-framework/issues/8621 .
// Reassigning a variable must not leave a type that names it unchanged.  After the reassignment,
// such a type would describe the new value of the variable, not the value for which the type was
// established.
// * A refined type that names the reassigned variable is discarded.
// * An assignment that might make the declared type of a local variable or formal parameter false
//   is an error.

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
      // :: error: [array.access.unsafe.high.range]
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
    // :: error: [array.access.unsafe.high.range]
    a[i] = 0;
  }

  // The declared type of the parameter `i` names the parameter `a`.
  void declaredParameterNamesParameter(@IndexFor("#2") int i, int[] a, int[] c) {
    // :: error: [reassignment.invalidates.type]
    a = c;
    a[i] = 0;
  }

  // The declared type of the parameter `i` names the field `f`.
  void declaredParameterNamesField(@IndexFor("f") int i, int[] c) {
    // :: error: [reassignment.invalidates.type]
    f = c;
    f[i] = 0;
  }

  // The refined type of `i` names the field `f`, which is reassigned.
  void refinedFieldFact(int i, int[] c) {
    if (i >= 0 && i < f.length) {
      f = c;
      // :: error: [array.access.unsafe.high.range]
      f[i] = 0;
    }
  }

  // The declared type of the outer array `matrix` is unaffected, but the declared type of its
  // components names `a`.
  void declaredNestedSameLen(
      int[] a, int[] b, int[] c, int @SameLen("#3") [] @SameLen("#1") [] matrix) {
    // :: error: [reassignment.invalidates.type]
    a = b;
  }

  // A local variable declared in an earlier case of a switch expression is in scope.
  int declaredInEarlierCaseOfSwitchExpression(int[] a, int[] c, int n) {
    return switch (n) {
      case 0:
        @LTEqLengthOf("a") int i = a.length;
        yield i;
      default:
        // :: error: [reassignment.invalidates.type]
        a = c;
        yield 0;
    };
  }

  // A local variable declared in an earlier case of a switch statement is in scope.
  void declaredInEarlierCaseOfSwitchStatement(int[] a, int[] c, int n) {
    switch (n) {
      case 0:
        @LTEqLengthOf("a") int i = a.length;
        break;
      default:
        // :: error: [reassignment.invalidates.type]
        a = c;
    }
  }

  // The declared type of the local variable `i` names the local variable `a`.
  void declaredLocal(int[] a, int[] c) {
    if (a.length == 0) {
      return;
    }
    @IndexFor("a") int i = a.length - 1;
    // :: error: [reassignment.invalidates.type]
    a = c;
    a[i] = 0;
  }

  // The offset in the declared type of `i` names the parameter `k`.
  void declaredOffset(
      @NonNegative @LTLengthOf(value = "#2", offset = "#3") int i, int[] a, @NonNegative int k) {
    // :: error: [reassignment.invalidates.type]
    k = 100;
    a[i + k] = 0;
  }

  // The Less Than Checker refines `i` to `@LessThan("limit")`, and the declared type of `limit`
  // names `a`, which `a = c` reassigned before `limit` was compared to the length of the new array.
  void declaredLimitThroughLessThan(@LTEqLengthOf("#2") int limit, int[] a, int[] c) {
    // :: error: [reassignment.invalidates.type]
    a = c;
    int i = a.length;
    if (limit > i) {
      a[i] = 0;
    }
  }

  // The declared type of `b` names `a`.
  void declaredSameLen(int[] a, int @SameLen("#1") [] b, int[] c) {
    // :: error: [reassignment.invalidates.type]
    a = c;
    int @SameLen("a") [] x = b;
  }

  // The declared type of `i` names `j`.
  void declaredLessThan(@LessThan("#2") int i, int j) {
    // :: error: [reassignment.invalidates.type]
    j = 0;
    @LessThan("j") int k = i;
  }

  // An increment of a variable that a declared type names is an error, because the new value is
  // expressed in terms of the old one.
  void declaredIncrement(
      @NonNegative @LTLengthOf(value = "#2", offset = "#3") int i, int[] a, int k) {
    // :: error: [reassignment.invalidates.type]
    k++;
    // :: error: [reassignment.invalidates.type]
    k += 2;
  }

  // The new value is expressed in terms of the old value of the reassigned variable.
  void declaredInTermsOfOldValue(@LessThan("#2") int i, int j) {
    // :: error: [reassignment.invalidates.type]
    j = j + 1;
  }

  //
  // Reassignments that must be accepted
  //

  // No declared type names `a`.
  void noDeclaredTypeNamesReassignedVariable(@IndexFor("#2") int i, int[] a, int[] b, int[] c) {
    b = c;
    a[i] = 0;
  }

  // A self-assignment does not change the value.
  void selfAssignment(@IndexFor("#2") int i, int[] a) {
    a = a;
    a[i] = 0;
  }

  // `b` has the same length as `a`, so `i` is still an index for `a` after `a = b`.
  void sameLenArray(@IndexFor("#2") int i, int[] a, int @SameLen("#2") [] b) {
    a = b;
    a[i] = 0;
  }

  // `b` and `c` have the same length as `a`, so `b` still has the same length as `a` after `a = c`.
  void sameLenOfSameLen(int[] a, int @SameLen("#1") [] b, int @SameLen("#1") [] c) {
    a = c;
    int @SameLen("a") [] x = b;
  }

  // `i` is less than the length of `c`, so `i` is still an index for `a` after `a = c`.
  void refinedForNewValue(@IndexFor("#2") int i, int[] a, int[] c) {
    if (i < c.length) {
      a = c;
      a[i] = 0;
    }
  }

  // No case declaration is in scope in the selector of a switch.
  void reassignmentInSwitchSelector(int[] a, int[] c, int n) {
    switch ((a = c).length) {
      case 0:
        @LTEqLengthOf("a") int i = a.length;
        break;
      default:
        break;
    }
  }

  // A refined type that does not name the reassigned variable survives the reassignment.
  void refinedFactNotNamingReassignedVariable(int[] a, int[] b, int[] c, int i) {
    if (i >= 0 && i < a.length) {
      b = c;
      a[i] = 0;
    }
  }

  // A refined type of a different variable that names a different array survives.
  void refinedFactOfOtherVariableSurvives(int[] a, int[] b, int[] c, int i, int j) {
    if (i >= 0 && i < a.length && j >= 0 && j < b.length) {
      b = c;
      a[i] = 0;
      // :: error: [array.access.unsafe.high.range]
      b[j] = 0;
    }
  }
}
