import org.checkerframework.checker.upperbound.qual.*;

class Issue20 {
    // An issue with LUB that results in losing information when unifying.
    int[] a, b;

    void test(@LtLength("a") int i, @LteLength({"a", "b"}) int j, boolean flag) {
        @LteLength("a")
        int k = flag ? i : j;
    }
}
