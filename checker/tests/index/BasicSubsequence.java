import org.checkerframework.checker.index.qual.*;

public class BasicSubsequence {
    // :: error: not.final
    @HasSubsequence(subsequence = "this", from = "this.x", to = "this.y")
    int[] b;

    int x;
    int y;

    void test2(@NonNegative @LessThan("y + 1") int x1, int[] a) {
        x = x1;
        // :: error: to.not.ltel
        b = a;
    }

    void test3(@NonNegative @LessThan("y") int x1, int[] a) {
        x = x1;
        // :: error: to.not.ltel
        b = a;
    }

    void test4(@NonNegative int x1, int[] a) {
        x = x1;
        // :: error: from.gt.to :: error: to.not.ltel
        b = a;
    }

    void test5(@GTENegativeOne @LessThan("y + 1") int x1, int[] a) {
        x = x1;
        // :: error: from.not.nonnegative :: error: to.not.ltel
        b = a;
    }

    void test6(@GTENegativeOne int x1, int[] a) {
        x = x1;
        // :: error: from.not.nonnegative :: error: to.not.ltel :: error: from.gt.to
        b = a;
    }

    void test7(@IndexFor("this") @LessThan("y") int x1, @IndexOrHigh("this") int y1, int[] a) {
        x = x1;
        y = y1;
        // :: warning: which.subsequence
        b = a;
    }
}
