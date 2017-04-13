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

    void test_intrange_eq(@IntRange(from = 3, to = 10) int x, @IntRange(from = 1, to = 5) int y) {
        if (x == y) {
            @IntRange(from = 3, to = 5) int a = x;
            @IntRange(from = 3, to = 5) int b = y;
        } else {
            @IntRange(from = 6, to = 10)
            //:: error: (assignment.type.incompatible)
            int a = x;
            @IntRange(from = 1, to = 2)
            //:: error: (assignment.type.incompatible)
            int b = y;
        }
    }

    void test_intrange_eq2(@IntRange(from = 0, to = 10) int x, @IntRange(from = 0, to = 0) int y) {
        if (x == y) {
            @IntRange(from = 0, to = 0) int a = x;
            @IntRange(from = 0, to = 0) int b = y;
        } else {
            @IntRange(from = 1, to = 10) int a = x;
            @IntRange(from = 0, to = 0) int b = y;
        }
    }

    void test_intrange_eq3(@IntRange(from = 0, to = 10) int x, @IntVal(0) int y) {
        if (x == y) {
            @IntVal(0) int a = x;
            @IntVal(0) int b = y;
        } else {
            @IntRange(from = 1, to = 10) int a = x;
            @IntVal(0) int b = y;
        }

        if (y != x) {
            @IntRange(from = 1, to = 10) int a = x;
            @IntVal(0) int b = y;
        }
    }

    void test_intrange_neq(@IntRange(from = 3, to = 10) int x, @IntRange(from = 1, to = 5) int y) {
        if (x != y) {
            @IntRange(from = 6, to = 10)
            //:: error: (assignment.type.incompatible)
            int a = x;
            @IntRange(from = 1, to = 2)
            //:: error: (assignment.type.incompatible)
            int b = y;
        } else {
            @IntRange(from = 3, to = 5) int a = x;
            @IntRange(from = 3, to = 5) int b = y;
        }
    }

    void test_intrange_neq2(
            @IntRange(from = 3, to = 10) int x, @IntRange(from = 10, to = 10) int y) {
        if (x != y) {
            @IntRange(from = 3, to = 9) int a = x;
            @IntRange(from = 10, to = 10) int b = y;
        } else {
            @IntRange(from = 10, to = 10) int a = x;
            @IntRange(from = 10, to = 10) int b = y;
        }
    }

    void test_intrange_gt(@IntRange(from = 0, to = 10) int x, @IntRange(from = 5, to = 20) int y) {
        if (x > y) {
            @IntRange(from = 6, to = 10) int a = x;
            @IntRange(from = 5, to = 9) int b = y;
        } else {
            @IntRange(from = 0, to = 10) int a = x;
            @IntRange(from = 5, to = 20) int b = y;
        }
    }

    void test_intrange_gt2(@IntRange(from = 5, to = 10) int x, @IntRange(from = 2, to = 7) int y) {
        if (x > y) {
            @IntRange(from = 5, to = 10) int a = x;
            @IntRange(from = 2, to = 7) int b = y;

            @IntRange(from = 5, to = 7)
            //:: error: (assignment.type.incompatible)
            int c = x;
            @IntRange(from = 5, to = 7)
            //:: error: (assignment.type.incompatible)
            int d = y;
        } else {
            @IntRange(from = 5, to = 7) int a = x;
            @IntRange(from = 5, to = 7) int b = y;
        }
    }

    void test_intrange_lte(@IntRange(from = 0, to = 10) int x, @IntRange(from = 2, to = 7) int y) {
        if (x <= y) {
            @IntRange(from = 0, to = 7) int a = x;
            @IntRange(from = 2, to = 7) int b = y;
        } else {
            @IntRange(from = 3, to = 10) int a = x;
            @IntRange(from = 2, to = 7) int b = y;
        }
    }

    void test_intrange_lte2(@IntRange(from = 2, to = 7) int x, @IntRange(from = 5, to = 10) int y) {
        if (x <= y) {
            @IntRange(from = 2, to = 7) int a = x;
            @IntRange(from = 2, to = 10) int b = y;
        } else {
            @IntRange(from = 6, to = 7) int a = x;
            @IntRange(from = 5, to = 6) int b = y;
        }
    }

    void test_intrange_lt(@IntRange(from = 5, to = 10) int x, @IntRange(from = 2, to = 7) int y) {
        if (x < y) {
            @IntRange(from = 5, to = 6) int a = x;
            @IntRange(from = 6, to = 7) int b = y;
        } else {
            @IntRange(from = 5, to = 10) int a = x;
            @IntRange(from = 2, to = 7) int b = y;
        }
    }

    void test_intrange_lt2(@IntRange(from = 2, to = 7) int x, @IntRange(from = 5, to = 10) int y) {
        if (x < y) {
            @IntRange(from = 2, to = 7) int a = x;
            @IntRange(from = 2, to = 10) int b = y;
        } else {
            @IntRange(from = 5, to = 7) int a = x;
            @IntRange(from = 5, to = 7) int b = y;
        }
    }

    void test_intrange_gte(@IntRange(from = 0, to = 10) int x, @IntRange(from = 2, to = 7) int y) {
        if (x >= y) {
            @IntRange(from = 2, to = 10) int a = x;
            @IntRange(from = 2, to = 7) int b = y;
        } else {
            @IntRange(from = 0, to = 6) int a = x;
            @IntRange(from = 2, to = 7) int b = y;
        }
    }

    void test_intrange_gte2(@IntRange(from = 3, to = 5) int x, @IntRange(from = 2, to = 6) int y) {
        if (x >= y) {
            @IntRange(from = 3, to = 5) int a = x;
            @IntRange(from = 2, to = 6) int b = y;
        } else {
            @IntRange(from = 3, to = 5) int a = x;
            @IntRange(from = 4, to = 6) int b = y;
        }
    }
}
