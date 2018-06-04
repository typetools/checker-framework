import org.checkerframework.checker.index.qual.HasSubsequence;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.LessThan;

@SuppressWarnings("lowerbound")
class BasicSubsequence3 {
    @HasSubsequence(value = "this", from = "this.start", to = "this.end")
    int[] array;

    @HasSubsequence(value = "this", from = "start", to = "end")
    int[] array2;

    @IndexFor("array") int start;

    @IndexOrHigh("array") int end;

    void testStartIndex(@IndexFor("array") @LessThan("this.end") int x) {
        @IndexFor("this") int y = x - start;
    }

    void testViewpointAdaption(@IndexFor("array2") @LessThan("end") int x) {
        @IndexFor("this") int y = x - start;
    }
}
