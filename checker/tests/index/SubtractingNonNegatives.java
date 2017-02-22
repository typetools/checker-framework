// Test case for issue 98: https://github.com/kelloggm/checker-framework/issues/98

import org.checkerframework.checker.index.qual.*;

public class SubtractingNonNegatives {
    public static void m4(int[] a, @IndexFor("#1") int i, @IndexFor("#1") int j) {
        int k = i;
        if (k >= j) {
            @IndexFor("a") int y = k;
        }
        for (k = i; k >= j; k -= j) {
            @IndexFor("a") int x = k;
        }
    }
}
