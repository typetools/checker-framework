import org.checkerframework.checker.index.qual.HasSubsequence;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;

class OnlyCheckSubsequenceWhenAssigningToArray {
    @HasSubsequence(subsequence = "this", from = "this.start", to = "this.end")
    int[] array;

    final @IndexFor("array") int start;

    final @IndexOrHigh("array") int end;

    private OnlyCheckSubsequenceWhenAssigningToArray(
            @IndexFor("array") int s, @IndexOrHigh("array") int e) {
        start = s;
        end = e;
    }

    void testAssignmentToArrayElement(@IndexFor("this") int x, int y) {
        array[start + x] = y;
    }

    void testAssignmentToArray(int[] a) {
        // :: error: to.not.ltel :: error: from.gt.to
        array = a;
    }
}
