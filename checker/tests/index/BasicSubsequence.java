import org.checkerframework.checker.index.qual.*;

class BasicSubsequence {
    void test1(int[] a) {
        // this test doesn't work - 0 isn't a valid flow expression, apparently.
        // int @HasSubsequence(value="a", from="0", to="a.length")[] b = a;
    }

    void test2(@NonNegative @LessThan("#2.length + 1") int x, int[] a) {
        // :: error: to.not.ltel
        @HasSubsequence(value = "a", from = "x", to = "a.length")
        int[] b = a;
    }

    void test3(@NonNegative @LessThan("#2.length") int x, int[] a) {
        // :: error: to.not.ltel
        @HasSubsequence(value = "a", from = "x", to = "a.length")
        int[] b = a;
    }

    void test4(@NonNegative int x, int[] a) {
        // :: error: from.gt.to
        @HasSubsequence(value = "a", from = "x", to = "a.length")
        int[] b = a;
    }

    void test5(@GTENegativeOne @LessThan("#2.length + 1") int x, int[] a) {
        // :: error: from.not.nonnegative
        @HasSubsequence(value = "a", from = "x", to = "a.length")
        int[] b = a;
    }

    void test6(@GTENegativeOne int x, int[] a) {
        // :: error: from.not.nonnegative
        @HasSubsequence(value = "a", from = "x", to = "a.length")
        int[] b = a;
    }

    void test7(@IndexFor("#3") @LessThan("#2") int x, @IndexOrHigh("#3") int y, int[] a) {
        // :: warning: which.subsequence
        @HasSubsequence(value = "a", from = "x", to = "y")
        int[] b = a;
    }
}
