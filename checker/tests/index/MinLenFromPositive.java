import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.common.value.qual.*;

class MinLenFromPositive {

    void test(@Positive int x) {
        int @MinLen(1) [] y = new int[x];
        @IntRange(from = 1) int z = x;
        @Positive int q = x;
    }

    @SuppressWarnings("index")
    void foo(int x) {
        test(x);
    }

    void foo2(int x) {
        //:: error: (argument.type.incompatible)
        test(x);
    }
}
