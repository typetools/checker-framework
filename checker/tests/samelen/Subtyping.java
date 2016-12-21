import org.checkerframework.checker.samelen.qual.*;

// This test checks whether the SameLen type system works as expected.

class Subtyping {

    void subtype(int @SameLen("b") [] a, int @SameLen("c") [] b) {
        int @SameLen({"a", "b"}) [] c = a;
        int @SameLen("c") [] d = b;

        //:: error: (assignment.type.incompatible)
        int @SameLen("f") [] e = a;
    }
}
