/*
 * Test case for Issue 138:
 * https://code.google.com/p/checker-framework/issues/detail?id=138
 *
 * Assignment and compound assignment should be treated equally for
 * method type argument inference.
 */
class Compound {
    public static void one() {
        long total = 0;
        total = two();
        total += two();
    }

    private static <N> long two() {
        return 0;
    }
}