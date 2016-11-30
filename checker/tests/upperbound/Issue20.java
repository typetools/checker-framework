import org.checkerframework.checker.upperbound.qual.*;

class Issue20 {
    // An issue with LUB that results in losing information when unifying.
    int[] a, b;

    void test(
            @LTLengthOf("a") int i, @LTEqLengthOf({"a", "b"}) int j, boolean flag) {
        @LTEqLengthOf("a") int k = flag ? i : j;
    }
}
