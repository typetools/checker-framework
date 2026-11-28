// Test case for https://github.com/typetools/checker-framework/issues/8621 .
// Reassigning a variable must not leave a type that names it unchanged.  After the reassignment,
// such a type would describe the new value of the variable, not the value for which the type was
// established.  A refined type that names the reassigned variable is discarded.

import org.checkerframework.checker.index.qual.LessThan;

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

  // The refined type of `i` names the field `f`, which is reassigned.
  void refinedFieldFact(int i, int[] c) {
    if (i >= 0 && i < f.length) {
      f = c;
      // :: error: [array.access.unsafe.high.range]
      f[i] = 0;
    }
  }

  //
  // Reassignments that must be accepted
  //

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

  // A self-assignment does not discard a refined type that names the variable.
  void selfAssignmentKeepsRefinement(int[] a, int i) {
    if (i >= 0 && i < a.length) {
      a = a;
      a[i] = 0;
    }
  }

  static class HasLengthField {
    int length;

    // The `length` of an array is not the field `length`.
    void arrayLengthRefinementSurvives(int[] a, int i) {
      if (i < a.length) {
        length = 3;
        @LessThan("a.length") int j = i;
      }
    }
  }
}
