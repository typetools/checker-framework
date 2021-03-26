import org.checkerframework.checker.index.qual.SameLen;

// This test checks whether the SameLen type system works as expected.

public class SLSubtyping {
  int[] f = {1};

  void subtype(int @SameLen("#2") [] a, int[] b) {
    int @SameLen({"a", "b"}) [] c = a;

    // :: error: (assignment.type.incompatible)
    int @SameLen("c") [] q = {1, 2};
    int @SameLen("c") [] d = q;

    // :: error: (assignment.type.incompatible)
    int @SameLen("f") [] e = a;
  }

  void subtype2(int[] a, int @SameLen("#1") [] b) {
    a = b;
    int @SameLen("b") [] c = b;
    int @SameLen("f") [] d = f;
  }
}
