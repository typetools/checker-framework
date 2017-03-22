import org.checkerframework.common.value.qual.*;

class Refinement2 {

    void eq_test(int x, @IntVal({1, 5, 6}) int w) {
        if (x == 1) {
            //:: error: (assignment.type.incompatible)
            @BottomVal int q = x;

        } else if (x == 2) {
        } else {
            return;
        }

        if (x != 1) {
            //:: error: (assignment.type.incompatible)
            @IntVal({1}) int y = x;
            @IntVal({2}) int z = x;
        }

        if (x == 1) {

            @IntVal({1}) int y = x;

            //:: error: (assignment.type.incompatible)
            @IntVal({2}) int z = x;
        } else {
            @IntVal({2}) int y = x;

            //:: error: (assignment.type.incompatible)
            @IntVal({1}) int z = x;
        }

        if (x == w) {
            @IntVal({1}) int y = x;
            @IntVal({1}) int z = w;
        } else {
            // These two assignments are illegal because one of x or w could be 1,
            // while the other is not. So no refinement can be applied.

            //:: error: (assignment.type.incompatible)
            @IntVal({2}) int y = x;
            //:: error: (assignment.type.incompatible)
            @IntVal({5, 6}) int z = w;
        }
    }

    void testDeadCode(int x) {
        if (x != 1 || x != 2) {
            return;
        }

        if (x == 3) {
            // This is actually dead code, but this assignment should pass.
            @IntVal(3) int y = x;
            //:: error: (assignment.type.incompatible)
            @BottomVal int z = x;
        }
    }

    void testArrayLen(boolean flag) {
        int[] a;
        if (flag) {
            a = new int[] {1};
        } else {
            a = new int[] {1, 2};
        }

        if (a.length != 1) {
            int @ArrayLen(2) [] b = a;
        }

        if (a.length == 3) {
            // Dead code
            int @ArrayLen(3) [] b = a;
            //:: error: (assignment.type.incompatible)
            int @BottomVal [] b = a;
        }
    }
}
