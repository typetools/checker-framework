package value;

import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;

public class LubToRange {
    public static boolean flag = false;

    void test(@IntVal({1, 2, 3, 4, 5}) int x, @IntVal({6, 7, 8, 9, 10, 11}) int y) {
        @IntRange(from = 1, to = 11) int z = flag ? x : y;
    }

    void test2(int @ArrayLen({1, 2, 3, 4, 5}) [] x, int @ArrayLen({6, 7, 8, 9, 10, 11}) [] y) {
        int @ArrayLenRange(from = 1, to = 11) [] z = flag ? x : y;
    }
}
