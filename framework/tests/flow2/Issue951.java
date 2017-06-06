// Test case for issue #951:
// https://github.com/typetools/checker-framework/issues/951

// @skip-test until the issue is fixed

public class Issue951 {

    @Pure
    public static int min(int[] a) {
        if (a.length == 0) {
            throw new ArrayIndexOutOfBoundsException("Empty array passed to min(int[])");
        }
        int result = a[0];
        for (int i = 1; i < a.length; i++) {
            result = Math.min(result, a[i]);
        }
        return result;
    }
}
