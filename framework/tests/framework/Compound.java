/*
 * Test case for Issue 138:
 * https://github.com/typetools/checker-framework/issues/138
 *
 * Assignment and compound assignment should be treated equally for
 * method type argument inference.
 */
public class Compound {
    public static void one() {
        long total = 0;
        total = two();
        total += two();
    }

    private static <N> long two() {
        return 0;
    }
}
