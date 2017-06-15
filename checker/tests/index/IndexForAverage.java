// test case for issue 86: https://github.com/kelloggm/checker-framework/issues/86

import org.checkerframework.checker.index.qual.*;

public class IndexForAverage {

    public static @IndexFor("#1") int bug(int[] a, @IndexFor("#1") int i, @IndexFor("#1") int j) {
        return (i + j) / 2;
    }

    public static @LTLengthOf("#1") int bug2(
            int[] a, @IndexFor("#1") int i, @IndexFor("#1") int j) {
        return ((i - 1) + j) / 2;
    }

    public static @LTLengthOf("#1") int bug3(
            int[] a, @IndexFor("#1") int i, @IndexFor("#1") int j) {
        //:: error: (return.type.incompatible)
        return ((i + 1) + j) / 2;
    }
}
