import org.checkerframework.checker.index.qual.*;

class ConstantArrays {
    void test() {
        int[] b = new int[4];
        @LTLengthOf("b") int[] a = {0, 1, 2, 3};
    }
}
