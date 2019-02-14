import org.checkerframework.checker.index.qual.HasSubsequence;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LessThan;

@SuppressWarnings("lowerbound")
class BasicSubsequence3 {
    @HasSubsequence(subsequence = "this", from = "this.start", to = "this.end")
    int[] array;

    @HasSubsequence(subsequence = "this", from = "start", to = "end")
    int[] array2;

    final @IndexFor("array") int start;

    final @IndexOrHigh("array") int end;

    private BasicSubsequence3(@IndexFor("array") int s, @IndexOrHigh("array") int e) {
        start = s;
        end = e;
    }

    void testStartIndex(@IndexFor("array") @LessThan("this.end") int x) {
        @IndexFor("this") int y = x - start;
    }

    void testViewpointAdaption(@IndexFor("array2") @LessThan("end") int x) {
        @IndexFor("this") int y = x - start;
    }
}
