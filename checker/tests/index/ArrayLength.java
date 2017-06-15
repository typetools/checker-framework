import org.checkerframework.checker.index.qual.LTEqLengthOf;

class ArrayLength {
    @SuppressWarnings("local.variable.unsafe.dependent.annotation")
    void test() {
        int[] arr = {1, 2, 3};
        @LTEqLengthOf({"arr"}) int a = arr.length;
    }

    private int[] b;

    @LTEqLengthOf("b") int test1() {
        b = new int[] {1, 2, 3};
        return b.length;
    }
}
