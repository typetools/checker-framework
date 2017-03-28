import org.checkerframework.checker.index.qual.*;
import org.checkerframework.framework.qual.PolyAll;

class Polymorphic2 {
    public static boolean flag = false;

    @PolyAll int merge(@PolyAll int a, @PolyAll int b) {
        return flag ? a : b;
    }

    int @PolyAll [] merge(int @PolyAll [] a, int @PolyAll [] b) {
        return flag ? a : b;
    }

    int @PolyMinLen [] mergeMinLen(int @PolyMinLen [] a, int @PolyMinLen [] b) {
        return flag ? a : b;
    }

    void testMinLen(int @MinLen(2) [] a, int @MinLen(5) [] b) {
        int @MinLen(2) [] x = merge(a, b);
        //:: error: (assignment.type.incompatible)
        int @MinLen(5) [] y = merge(a, b);

        int @MinLen(2) [] z = mergeMinLen(a, b);
        //:: error: (assignment.type.incompatible)
        int @MinLen(5) [] zz = mergeMinLen(a, b);
    }

    int @PolySameLen [] mergeSameLen(int @PolySameLen [] a, int @PolySameLen [] b) {
        return flag ? a : b;
    }

    int[] array1 = new int[2];
    int[] array2 = new int[2];

    void testSameLen(int @SameLen("array1") [] a, int @SameLen("array2") [] b) {
        int[] x = merge(a, b);
        //:: error: (assignment.type.incompatible)
        int @SameLen("array1") [] y = merge(a, b);

        int[] z = mergeMinLen(a, b);
        //:: error: (assignment.type.incompatible)
        int @SameLen("array1") [] zz = mergeMinLen(a, b);
    }

    @PolyUpperBound int mergeUpperBound(@PolyUpperBound int a, @PolyUpperBound int b) {
        return flag ? a : b;
    }
    // UpperBound tests
    void testUpperBound(@LTLengthOf("array1") int a, @LTLengthOf("array2") int b) {
        int x = merge(a, b);
        //:: error: (assignment.type.incompatible)
        @LTLengthOf("array1") int y = merge(a, b);

        int z = mergeUpperBound(a, b);
        //:: error: (assignment.type.incompatible)
        @LTLengthOf("array1") int zz = mergeUpperBound(a, b);
    }

    void testUpperBound2(@LTLengthOf("array1") int a, @LTEqLengthOf("array1") int b) {
        @LTEqLengthOf("array1") int x = merge(a, b);
        //:: error: (assignment.type.incompatible)
        @LTLengthOf("array1") int y = merge(a, b);

        @LTEqLengthOf("array1") int z = mergeUpperBound(a, b);
        //:: error: (assignment.type.incompatible)
        @LTLengthOf("array1") int zz = mergeUpperBound(a, b);
    }

    @PolyLowerBound int mergeLowerBound(@PolyLowerBound int a, @PolyLowerBound int b) {
        return flag ? a : b;
    }
    // LowerBound tests
    void lbc_id(@NonNegative int n, @Positive int p) {
        @NonNegative int x = merge(n, p);
        //:: error: (assignment.type.incompatible)
        @Positive int y = merge(n, p);

        @NonNegative int z = mergeLowerBound(n, p);
        //:: error: (assignment.type.incompatible)
        @Positive int zz = mergeLowerBound(n, p);
    }
}
