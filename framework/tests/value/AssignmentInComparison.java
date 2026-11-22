import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.IntVal;

// A comparison of an assignment refines only the sides that have the assigned value afterward.
public class AssignmentInComparison {

  static void localTarget(int i, int j) {
    if ((i = j) == 3) {
      @IntVal(3) int x = i;
      @IntVal(3) int y = j;
    }
  }

  static void indexReassigned(int i, int[] a) {
    if ((i = a[i]) == 3) {
      @IntVal(3) int x = i;
      // After the assignment, `a[i]` is a different element of `a`.
      // :: error: [assignment]
      @IntVal(3) int y = a[i];
    }
  }

  static void otherOperandReassignsIndex(int[] a, int i) {
    if (a[i] == (i = 5)) {
      @IntVal(5) int x = i;
      // The left operand is an element of `a` at the old index `i`.
      // :: error: [assignment]
      @IntVal(5) int y = a[i];
    }
  }

  static void assignmentInTargetIndexBeforeRhs(int[] a, int[] b, int i) {
    if ((a[i = 0] = b[i]) == 3) {
      // `b[i]` is evaluated after `i = 0`, so it has the assigned value.
      @IntVal(3) int x = b[i];
    }
  }

  static void arrayTargetOtherArrayType(int[] a, long[] b, int[] idx) {
    // An assignment to an element of `b` does not change `idx[0]`, because `b` and `idx` cannot
    // alias.
    if ((b[0] = a[idx[0]]) == 3) {
      @IntVal(3) long x = b[0];
      @IntVal(3) int y = a[idx[0]];
    }
  }

  static void arrayTargetSameArrayType(int[] a, int[] b, int[] idx) {
    if ((b[0] = a[idx[0]]) == 3) {
      @IntVal(3) int x = b[0];
      // The assignment to `b[0]` might change `idx[0]`, if `b` and `idx` alias.
      // :: error: [assignment]
      @IntVal(3) int y = a[idx[0]];
    }
  }

  static class Box {
    int f;
  }

  static void stringLengthAcrossFieldWrite(String s, Box box) {
    // The write to `box.f` cannot change `s.length()`.
    if (s.length() == (box.f = 3)) {
      @ArrayLen(3) String exact = s;
    }
  }

  static void stringLengthOfReassignedReceiver(String s, String t) {
    if (s.length() == ((s = t) == null ? 3 : 3)) {
      // `s` refers to `t` now, not to the string whose length is 3.
      // :: error: [assignment]
      @ArrayLen(3) String exact = s;
    }
  }

  static void wideningOfAssignment(int i, int j) {
    if ((i = j) == 3L) {
      @IntVal(3) int x = i;
      @IntVal(3) int y = j;
    }
  }

  static void annotationArgumentInRhs(int[] a, int[] b) {
    // The annotation argument `{0, 1}` is not an assignment to a variable.
    if ((a = new @IntVal({0, 1}) int[] {0, 1}) == b) {
      int @ArrayLen(2) [] x = b;
    }
  }
}
