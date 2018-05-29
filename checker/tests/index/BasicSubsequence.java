import org.checkerframework.checker.index.qual.*;

class BasicSubsequence {
    void test1(int[] a) {
        // this test doesn't work - 0 isn't a valid flow expression, apparently.
        // int @HasSubsequence(value="a", from="0", to="a.length")[] b = a;
    }

    void test2(@NonNegative @LessThan("#2.length + 1") int x, int[] a) {
        @HasSubsequence(value = "a", from = "x", to = "a.length")
        // :: error: to.not.ltel
        int[] b = a;
    }

    void test3(@NonNegative @LessThan("#2.length") int x, int[] a) {
        @HasSubsequence(value = "a", from = "x", to = "a.length")
        // :: error: to.not.ltel
        int[] b = a;
    }

    void test4(@NonNegative int x, int[] a) {
        @HasSubsequence(value = "a", from = "x", to = "a.length")
        // :: error: from.gt.to :: error: to.not.ltel
        int[] b = a;
    }

    void test5(@GTENegativeOne @LessThan("#2.length + 1") int x, int[] a) {
        @HasSubsequence(value = "a", from = "x", to = "a.length")
        // :: error: from.not.nonnegative :: error: to.not.ltel
        int[] b = a;
    }

    void test6(@GTENegativeOne int x, int[] a) {
        @HasSubsequence(value = "a", from = "x", to = "a.length")
        // :: error: from.not.nonnegative :: error: to.not.ltel :: error: from.gt.to
        int[] b = a;
    }

    void test7(@IndexFor("#3") @LessThan("#2") int x, @IndexOrHigh("#3") int y, int[] a) {
        @HasSubsequence(value = "a", from = "x", to = "y")
        // :: warning: which.subsequence
        int[] b = a;
    }
}
