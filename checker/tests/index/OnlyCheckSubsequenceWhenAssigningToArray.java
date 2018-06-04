import org.checkerframework.checker.index.qual.HasSubsequence;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;

class OnlyCheckSubsequenceWhenAssigningToArray {
    @HasSubsequence(value = "this", from = "this.start", to = "this.end")
    int[] array;

    @IndexFor("array") int start;

    @IndexOrHigh("array") int end;

    void testAssignmentToArrayElement(@IndexFor("this") int x, int y) {
        array[start + x] = y;
    }

    void testAssignmentToArray(int[] a) {
        // :: error: to.not.ltel :: error: from.gt.to
        array = a;
    }
}
