// Test case for Issue 867:
// https://github.com/typetools/checker-framework/issues/867

import org.checkerframework.common.value.qual.*;

class Issue867 {
    void test1() {
        @IntVal({0, 1}) int x = 0;
        @IntVal(0) int zero = x++;
        @IntVal(1) int one = x;
        // :: error: (unary.increment.type.incompatible)
        x++;

        x = 1;
        one = x--;
        zero = x;
        // :: error: (unary.decrement.type.incompatible)
        x--;
    }

    void test2() {
        @IntVal({0, 1, 2}) int x = 0;
        @IntVal(1) int one = x++ + x++;
        @IntVal(2) int two = x;
        // :: error: (unary.increment.type.incompatible)
        x++;

        x = 2;
        @IntVal(3) int three = x-- + x--;
        @IntVal(0) int zero = x;
        // :: error: (unary.decrement.type.incompatible)
        x--;
    }

    void test3() {
        @IntVal({0, 1, 2}) int x = 0;
        @IntVal(2) int two = x++ + ++x;
        two = x;
        // :: error: (unary.increment.type.incompatible)
        x++;

        x = 2;
        two = x-- + --x;
        @IntVal(0) int zero = x;
        // :: error: (unary.decrement.type.incompatible)
        x--;
    }

    void test4() {
        @IntVal({0, 1}) int x = 0;
        m0(x++);
        // :: error: (argument.type.incompatible)
        m0(x);
        // :: error: (unary.increment.type.incompatible)
        m1(x++);

        x = 1;
        m1(x--);
        // :: error: (argument.type.incompatible)
        m1(x);
        // :: error: (unary.decrement.type.incompatible)
        m0(x--);
    }

    void m0(@IntVal(0) int x) {}

    void m1(@IntVal(1) int x) {}
}
