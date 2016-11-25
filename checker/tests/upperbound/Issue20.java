import org.checkerframework.checker.upperbound.qual.*;

class Issue20 {
    // An issue with LUB that results in losing information when unifying.
    int[] a, b;

    void test(
            @LessThanLength("a") int i, @LessThanOrEqualToLength({"a", "b"}) int j, boolean flag) {
        @LessThanOrEqualToLength("a") int k = flag ? i : j;
    }
}
