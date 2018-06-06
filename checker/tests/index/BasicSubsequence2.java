import org.checkerframework.checker.index.qual.HasSubsequence;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;

class BasicSubsequence2 {
    @HasSubsequence(value = "this", from = "this.start", to = "this.end")
    int[] array;

    @HasSubsequence(value = "this", from = "start", to = "end")
    int[] array2;

    final @IndexFor("array") int start;

    final @IndexOrHigh("array") int end;

    private BasicSubsequence2(@IndexFor("array") int s, @IndexOrHigh("array") int e) {
        start = s;
        end = e;
    }

    void testStartIndex(@IndexFor("this") int x) {
        @IndexFor("array") int y = x + start;
    }

    void testViewpointAdaption(@IndexFor("this") int x) {
        @IndexFor("array2") int y = x + start;
    }

    void testArrayAccess(@IndexFor("this") int x) {
        int y = array[x + start];
    }

    void testCommutativity(@IndexFor("this") int x) {
        @IndexFor("array") int y = start + x;
    }
}
