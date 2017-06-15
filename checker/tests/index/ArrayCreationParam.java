// Test case for issue 93: https://github.com/kelloggm/checker-framework/issues/93

import org.checkerframework.checker.index.qual.*;

public class ArrayCreationParam {

    private int[] b;

    public static int m1() {
        int n = 5;
        int[] a = new int[n + 1];
        //Index Checker correctly issues no warning on the lines below
        for (int i = 1; i <= n; i++) {
            int x = a[i];
        }
        return n;
    }

    public @IndexFor("b") int m2() {
        int n = 5;
        b = new int[n + 1];
        //Index Checker correctly issues no warning on the lines below
        for (int i = 1; i <= n; i++) {
            int x = b[i];
        }
        return n;
    }
}
