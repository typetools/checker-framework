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

    int @PolyValue [] mergeMinLen(int @PolyValue [] a, int @PolyValue [] b) {
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

    void testArrayLen(int @ArrayLen(2) [] a, int @ArrayLen(5) [] b) {
        int @ArrayLen(2) [] x = merge(a, b);
        //:: error: (assignment.type.incompatible)
        int @ArrayLen(5) [] y = merge(a, b);

        int @ArrayLen(2) [] z = mergeMinLen(a, b);
        //:: error: (assignment.type.incompatible)
        int @ArrayLen(5) [] zz = mergeMinLen(a, b);
    }

    void testIntVal(@IntVal(2) int a, @IntVal(5) int b) {
        @IntVal(2) int x = merge(a, b);
        //:: error: (assignment.type.incompatible)
        @IntVal(5) int y = merge(a, b);

        @IntVal(2) int z = mergeMinLen(a, b);
        //:: error: (assignment.type.incompatible)
        @IntVal(5) int zz = mergeMinLen(a, b);
    }
}
