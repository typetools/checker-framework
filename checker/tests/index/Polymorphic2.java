import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.PolyLowerBound;
import org.checkerframework.checker.index.qual.PolySameLen;
import org.checkerframework.checker.index.qual.PolyUpperBound;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.framework.qual.PolyAll;

class Polymorphic2 {
    public static boolean flag = false;

    @PolyAll int merge(@PolyAll int a, @PolyAll int b) {
        return flag ? a : b;
    }

    int @PolyAll [] merge(int @PolyAll [] a, int @PolyAll [] b) {
        return flag ? a : b;
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
