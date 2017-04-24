import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.common.value.qual.*;

class MinLenFromPositive {

    void test(@Positive int x) {
        int @MinLen(1) [] y = new int[x];
        @IntRange(from = 1) int z = x;
        @Positive int q = x;
    }

    // Ensure that just running the value checker doesn't result in an LHS warning.
    void foo2(int x) {
        test(x);
    }
}
