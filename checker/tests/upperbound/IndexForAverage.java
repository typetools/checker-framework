// test case for issue 86: https://github.com/kelloggm/checker-framework/issues/86

// @skip-test until issue is fixed

import org.checkerframework.checker.index.qual.*;

public class IndexForAverage {

    public static void bug(int[] a, @IndexFor("#1") int i, @IndexFor("#1") int j) {
        @IndexFor("a") int k = (i + j) / 2;
    }
}
