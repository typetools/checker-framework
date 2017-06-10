// @skip-test until we solve the underlying issue. The code that caused this to
// pass has been removed, because an incomplete solution that masks the
// problem but still permits some unsoundness is worse than no solution and
// an obvious issue.

import org.checkerframework.checker.index.qual.*;

class Reassignment {

    private int[] b;

    @IndexFor("b") int bi;

    void test(int[] arr, int i, @IndexFor("b") int k) {
        if (i > 0 && i < arr.length) {
            arr = new int[0];
            //:: error: (array.access.unsafe.high)
            int j = arr[i];
            //:: error: (reassignment.not.permitted)
            b = new int[0];
        }
    }
}
