// Testcase for Issue 60
// https://github.com/kelloggm/checker-framework/issues/60

import org.checkerframework.checker.index.qual.IndexFor;

class Issue60 {

    public static int[] fn_compose(@IndexFor("#2") int[] a, int[] b) {
        int[] result = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            int inner = a[i];
            if (inner == -1) {
                result[i] = -1;
            } else {
                result[i] = b[inner];
            }
        }
        return result;
    }
}
