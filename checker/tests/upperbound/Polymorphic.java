// This test checks the polymorphic qualifier for the index checker.
// Because the UBC runs the rest of the checkers as subcheckers, it's
// tested with the UBC. If that ever changes, this file should be propagated
// appropriately.

import org.checkerframework.checker.index.qual.*;

class Polymorphic {

    //Identity functions

    int @PolyIndex [] array_identity(int @PolyIndex [] a) {
        return a;
    }

    @PolyIndex
    int int_identity(@PolyIndex int a) {
        return a;
    }

    // MinLen tests
    void minlen_id(int @MinLen(5) [] a) {
        int @MinLen(5) [] b = array_identity(a);
        //:: error: (assignment.type.incompatible)
        int @MinLen(6) [] c = array_identity(b);
    }

    // SameLen tests
    void samelen_id(int @SameLen("#2") [] a, int[] a2) {
        int[] banana;
        int @SameLen("a2") [] b = array_identity(a);
        //:: error: (assignment.type.incompatible)
        int @SameLen("banana") [] c = array_identity(b);
    }

    // LowerBound tests
    void lbc_id(@NonNegative int n, @Positive int p, @GTENegativeOne int g) {
        @NonNegative int an = int_identity(n);
        //:: error: (assignment.type.incompatible)
        @Positive int bn = int_identity(n);

        @GTENegativeOne int ag = int_identity(g);
        //:: error: (assignment.type.incompatible)
        @NonNegative int bg = int_identity(g);

        @Positive int ap = int_identity(p);
    }

    // UpperBound tests
    void ubc_id(
            int[] a,
            int[] b,
            @LTLengthOf("#1") int ai,
            @LTEqLengthOf("#1") int al,
            @LTLengthOf({"#1", "#2"}) int abi,
            @LTEqLengthOf({"#1", "#2"}) int abl) {
        int[] c;

        @LTLengthOf("a") int ai1 = int_identity(ai);
        //:: error: (assignment.type.incompatible)
        @LTLengthOf("b") int ai2 = int_identity(ai);

        @LTEqLengthOf("a") int al1 = int_identity(al);
        //:: error: (assignment.type.incompatible)
        @LTLengthOf("a") int al2 = int_identity(al);

        @LTLengthOf({"a", "b"}) int abi1 = int_identity(abi);
        //:: error: (assignment.type.incompatible)
        @LTLengthOf({"a", "b", "c"}) int abi2 = int_identity(abi);

        @LTEqLengthOf({"a", "b"}) int abl1 = int_identity(abl);
        //:: error: (assignment.type.incompatible)
        @LTEqLengthOf({"a", "b", "c"}) int abl2 = int_identity(abl);
    }
}
