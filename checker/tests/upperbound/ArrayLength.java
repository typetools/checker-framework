import org.checkerframework.checker.upperbound.qual.*;

class ArrayLength {
    void test() {
        int[] arr = {1, 2, 3};
        @LessThanOrEqualToLength({"arr"}) int a = arr.length;
    }
}
