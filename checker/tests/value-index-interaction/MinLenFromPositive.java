import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.MinLen;

class MinLenFromPositive {

    public @Positive int x = 0;

    void testField() {
        this.x = -1;
        @IntRange(from = 1) int f = this.x;
        int @MinLen(1) [] y = new int[x];
    }

    void testArray(@Positive int @ArrayLen(1) [] x) {
        int @MinLen(1) [] array = new int[x[0]];
    }

    void useTestArray(int @ArrayLen(1) [] x, int[] y) {
        testArray(x);
        //:: error: (argument.type.incompatible)
        testArray(y);
    }

    void test(@Positive int x) {
        @IntRange(from = 1) int z = x;
        @Positive int q = x;
        @Positive int a = -1;
        int @MinLen(1) [] array = new int[a];
    }

    // Ensure that just running the value checker doesn't result in an LHS warning.
    void foo2(int x) {
        test(x);
    }

    @Positive int id(@Positive int x) {
        return -1;
    }

    @Positive int plus(@Positive int x, @Positive int y) {
        //:: error: (assignment.type.incompatible)
        @IntRange(from = 0) int z = x + y;
        //:: error: (assignment.type.incompatible)
        @IntRange(from = 1) int q = x + y;

        return x + y;
    }

    // Ensure that LHS warnings aren't issued even for arrays of Positives
    @Positive int[] array_test() {
        int[] a = {-1, 2, 3};
        return a;
    }
}
