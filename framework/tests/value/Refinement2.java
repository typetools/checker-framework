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
        if (x == 4 || x == 5) {
            @IntVal({4, 5}) int a = x;
            //:: error: (assignment.type.incompatible)
            @BottomVal int a2 = x;
        }
        if (x == 4 && x == 5) {
            // This is dead, so x should be bottom.
            @IntVal({4, 5}) int a = x;
            @BottomVal int a2 = x;
        }
        if (x != 1 || x != 2) {
            return;
        }
        if (x != 2) {
            @IntVal(1) int a = x;
        }

        if (x == 3) {
            // This is actually dead code, so x is treated as bottom.
            @IntVal(3) int y = x;
            @IntVal(33) int v = x;
            @BottomVal int z = x;
        }
    }

    void simpleNull(@IntVal({1, 2, 3}) Integer x) {
        if (x == null) {
            @BottomVal int y = x;
        }
    }

    void moreTests(@IntVal({1, 2, 3}) Integer x, Integer y) {
        if (x == null) {
            if (y == x) {
                // x and y should be @BottomVal
                @BottomVal int z1 = x;
                @BottomVal int z2 = y;
            } else {
                // y should be @UnknownVal here.
                //:: error: (assignment.type.incompatible)
                @IntVal({1, 2, 3}) int z = y;
            }
        }

        if (x == null) {
            if (y < x) {
                // x should be @BottomVal
                @BottomVal int z1 = x;
                // This is dead code since the unboxing of x in the if statement
                // will throw a NPE, so the type of y doesn't matter.
                // @BottomVal int z2 = y;
            } else {
                // This is dead code, too.
            }
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
            int @ArrayLen(5) [] c = a;
            int @BottomVal [] bot = a;
        }
    }
}
