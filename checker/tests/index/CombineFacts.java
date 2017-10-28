import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.*;

class CombineFacts {

    @IndexFor("#1") int getLen(int @MinLen(1) [] a1) {
        return a1.length - 1;
    }

    void test(int @MinLen(1) [] a1) {
        int len = getLen(a1);
        int[] a2 = new int[len];
        a1[len] = 1;

        // This access should issue an error.
        //:: error: (array.access.unsafe.high)
        a2[len] = 1;
    }
}
