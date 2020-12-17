// @skip-test until we solve the underlying issue. The code that caused this to
// pass has been removed, because an incomplete solution that masks the
// problem but still permits some unsoundness is worse than no solution and
// an obvious issue.

public class Reassignment {
    void test(int[] arr, int i) {
        if (i > 0 && i < arr.length) {
            arr = new int[0];
            // :: error: (array.access.unsafe.high)
            int j = arr[i];
        }
    }
}
