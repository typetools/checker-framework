import org.checkerframework.common.value.qual.*;
import org.checkerframework.framework.qual.PolyAll;

class Polymorphic2 {
    public static boolean flag = false;

    @PolyAll int merge(@PolyAll int a, @PolyAll int b) {
        return flag ? a : b;
    }

    int @PolyAll [] merge(int @PolyAll [] a, int @PolyAll [] b) {
        return flag ? a : b;
    }

    @PolyValue int mergeValue(@PolyValue int a, @PolyValue int b) {
        return flag ? a : b;
    }

    int @PolyValue [] mergeValue(int @PolyValue [] a, int @PolyValue [] b) {
        return flag ? a : b;
    }

    void testMinLen(int @MinLen(2) [] a, int @MinLen(5) [] b) {
        int @MinLen(2) [] x = merge(a, b);
        // :: error: (assignment.type.incompatible)
        int @MinLen(5) [] y = merge(a, b);

        int @MinLen(2) [] z = mergeValue(a, b);
        // :: error: (assignment.type.incompatible)
        int @MinLen(5) [] zz = mergeValue(a, b);
    }

    void testArrayLen(int @ArrayLen(2) [] a, int @ArrayLen(5) [] b) {
        // :: error: (assignment.type.incompatible)
        int @ArrayLen(2) [] x = merge(a, b);
        // :: error: (assignment.type.incompatible)
        int @ArrayLen(5) [] y = merge(a, b);

        int @ArrayLen({2, 5}) [] yy = merge(a, b);

        // :: error: (assignment.type.incompatible)
        int @ArrayLen(2) [] z = mergeValue(a, b);
        // :: error: (assignment.type.incompatible)
        int @ArrayLen(5) [] zz = mergeValue(a, b);

        int @ArrayLen({2, 5}) [] zzz = mergeValue(a, b);
    }

    void testIntVal(@IntVal(2) int a, @IntVal(5) int b) {
        // :: error: (assignment.type.incompatible)
        @IntVal(2) int x = merge(a, b);
        // :: error: (assignment.type.incompatible)
        @IntVal(5) int y = merge(a, b);

        @IntVal({2, 5}) int yy = merge(a, b);

        // :: error: (assignment.type.incompatible)
        @IntVal(2) int z = mergeValue(a, b);
        // :: error: (assignment.type.incompatible)
        @IntVal(5) int zz = mergeValue(a, b);

        @IntVal({2, 5}) int zzz = mergeValue(a, b);
    }
}
