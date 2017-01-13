import org.checkerframework.checker.samelen.qual.*;

// This test checks whether the SameLen type system works as expected.

class Subtyping {
    int[] f = {1};

    void subtype(int @SameLen("#1") [] a, int[] b) {
        int @SameLen({"a", "b"}) [] c = a;

        //:: error: (assignment.type.incompatible)
        int @SameLen("c") [] q = {1, 2};
        int @SameLen("c") [] d = q;

        //:: error: (assignment.type.incompatible)
        int @SameLen("f") [] e = a;
    }
}
