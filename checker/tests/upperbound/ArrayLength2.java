// Test case for issue #14:
// https://github.com/kelloggm/checker-framework/issues/14

// @skip-test until the bug is fixed

import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.checker.upperbound.qual.*;

public class ArrayLength2 {
    public static void main(String[] args) {
        int N = 8;
        int @MinLen(8) [] Grid = new int[N];
        @LTLengthOf("Grid") int i = 0;
    }
}
