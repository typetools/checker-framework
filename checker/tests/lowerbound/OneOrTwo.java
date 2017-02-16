import org.checkerframework.common.value.qual.*;

class OneOrTwo {
    @IntVal({1, 2}) int getOneOrTwo() {
        return 1;
    }

    void test() {
        int[] a = new int[getOneOrTwo()];
    }
}
