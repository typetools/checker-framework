import org.checkerframework.checker.index.qual.HasSubsequence;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LessThan;

public class InvalidSubsequence {
    // :: error: flowexpr.parse.error :: error: not.final
    @HasSubsequence(subsequence = "banana", from = "this.from", to = "this.to")
    int[] a;

    // :: error: flowexpr.parse.error :: error: not.final
    @HasSubsequence(subsequence = "this", from = "banana", to = "this.to")
    int[] b;

    // :: error: flowexpr.parse.error :: error: not.final
    @HasSubsequence(subsequence = "this", from = "this.from", to = "banana")
    int[] c;

    // :: error: not.final
    @HasSubsequence(subsequence = "this", from = "this.from", to = "10")
    int[] e;

    @IndexFor("a") @LessThan("to") int from;

    @IndexOrHigh("a") int to;

    void assignA(int[] d) {
        // :: error: to.not.ltel
        a = d;
    }

    void assignB(int[] d) {
        // :: error: from.gt.to :: error: from.not.nonnegative :: error: to.not.ltel
        b = d;
    }

    void assignC(int[] d) {
        // :: error: from.gt.to :: error: to.not.ltel
        c = d;
    }

    void assignE(int[] d) {
        // :: error: from.gt.to :: error: to.not.ltel
        e = d;
    }
}
