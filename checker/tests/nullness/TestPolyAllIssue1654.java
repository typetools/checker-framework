// Test case for Issue 1654:
// https://github.com/typetools/checker-framework/issues/1654

// @skip-test until the issue is fixed

import org.checkerframework.framework.qual.PolyAll;

public class TestPolyAllIssue1654 {

    public static @PolyAll int[] fn_compose(int[] a, @PolyAll int[] b) {
        @PolyAll int[] result = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = b[a[i]];
        }
        return result;
    }
}
