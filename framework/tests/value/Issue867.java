// Test case for Issue 867:
// https://github.com/typetools/checker-framework/issues/867
import org.checkerframework.common.value.qual.*;

class Issue867 {
    void test1() {
        @IntVal({0, 1}) int x = 0;
        @IntVal(0) int y = x++;
        @IntVal(1) int w = x;
    }

    void test2() {
        @IntVal({0, 1, 2}) int x = 0;
        @IntVal(1) int y = x++ + x++;
        @IntVal(2) int w = x;
    }

    void test3() {
        @IntVal({0, 1, 2}) int x = 0;
        @IntVal(2) int y = x++ + ++x;
        @IntVal(2) int w = x;
    }

    void test4() {
        @IntVal({0, 1}) int x = 0;
        m(x++);
    }

    void m(@IntVal(0) int x) {}
}
