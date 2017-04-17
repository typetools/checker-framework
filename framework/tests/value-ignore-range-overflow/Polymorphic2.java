import org.checkerframework.common.value.qual.*;
import org.checkerframework.framework.qual.PolyAll;

//@skip-test

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
}
