import org.checkerframework.common.value.qual.*;

class Refinement {

    void eq_test(@IntVal({1, 2}) int x, @IntVal({1, 5, 6}) int w) {
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

    void lt_test(@IntVal({1, 2}) int x, @IntVal({1, 5, 6}) int w) {
        if (x < 2) {
            @IntVal({1}) int y = x;

            //:: error: (assignment.type.incompatible)
            @IntVal({2}) int z = x;
        } else {
            @IntVal({2}) int y = x;

            //:: error: (assignment.type.incompatible)
            @IntVal({1}) int z = x;
        }

        if (x < w) {
            //:: error: (assignment.type.incompatible)
            @IntVal({1}) int y = x;

            @IntVal({5, 6}) int z = w;
        } else {
            //:: error: (assignment.type.incompatible)
            @IntVal({2}) int y = x;
            @IntVal({1}) int z = w;
        }
    }

    void lte_test(@IntVal({1, 2}) int x, @IntVal({1, 5, 6}) int w) {
        if (x <= 1) {
            @IntVal({1}) int y = x;

            //:: error: (assignment.type.incompatible)
            @IntVal({2}) int z = x;
        } else {
            @IntVal({2}) int y = x;

            //:: error: (assignment.type.incompatible)
            @IntVal({1}) int z = x;
        }

        if (x <= w) {
            //:: error: (assignment.type.incompatible)
            @IntVal({1}) int y = x;
            //:: error: (assignment.type.incompatible)
            @IntVal({5, 6}) int z = w;
        } else {
            @IntVal({2}) int y = x;
            @IntVal({1}) int z = w;
        }
    }

    void neq_test(@IntVal({1, 2}) int x, @IntVal({1, 5, 6}) int w) {
        if (x != 1) {
            @IntVal({2}) int y = x;

            //:: error: (assignment.type.incompatible)
            @IntVal({1}) int z = x;
        } else {
            @IntVal({1}) int y = x;

            //:: error: (assignment.type.incompatible)
            @IntVal({2}) int z = x;
        }

        if (x != w) {
            // These two assignments are illegal because one of x or w could be 1,
            // while the other is not. So no refinement can be applied.

            //:: error: (assignment.type.incompatible)
            @IntVal({2}) int y = x;
            //:: error: (assignment.type.incompatible)
            @IntVal({5, 6}) int z = w;
        } else {
            @IntVal({1}) int y = x;
            @IntVal({1}) int z = w;
        }
    }

    void gte_test(@IntVal({1, 2}) int x, @IntVal({1, 5, 6}) int w) {
        if (x >= 2) {
            @IntVal({2}) int y = x;

            //:: error: (assignment.type.incompatible)
            @IntVal({1}) int z = x;
        } else {
            @IntVal({1}) int y = x;

            //:: error: (assignment.type.incompatible)
            @IntVal({2}) int z = x;
        }

        if (x >= w) {
            //:: error: (assignment.type.incompatible)
            @IntVal({2}) int y = x;
            @IntVal({1}) int z = w;
        } else {
            //:: error: (assignment.type.incompatible)
            @IntVal({1}) int y = x;

            @IntVal({5, 6}) int z = w;
        }
    }

    void gt_test(@IntVal({1, 2}) int x, @IntVal({1, 5, 6}) int w) {
        if (x > 1) {
            @IntVal({2}) int y = x;

            //:: error: (assignment.type.incompatible)
            @IntVal({1}) int z = x;
        } else {
            @IntVal({1}) int y = x;

            //:: error: (assignment.type.incompatible)
            @IntVal({2}) int z = x;
        }

        if (x > w) {
            @IntVal({2}) int y = x;
            @IntVal({1}) int z = w;
        } else {
            //:: error: (assignment.type.incompatible)
            @IntVal({1}) int y = x;
            //:: error: (assignment.type.incompatible)
            @IntVal({5, 6}) int z = w;
        }
    }

    void boolean_test(@IntVal({1, 2}) int x, @IntVal({1, 5, 6}) int w) {
        @BoolVal({true}) boolean b = x >= 0;
        @BoolVal({false}) boolean c = w == 3;
        @BoolVal({true, false}) boolean d = x < w;
    }
}
