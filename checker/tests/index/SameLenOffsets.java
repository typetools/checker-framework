// This class is a basic test case for SameLen with offsets. A more complete test case is in GuavaPrimitives.java.

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.IndexOrHigh;
import org.checkerframework.checker.index.qual.IndexOrLow;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.SameLen;

class SameLenOffsets {

    @IndexOrHigh("a") @LessThan("end + 1")
    int start;

    @IndexOrHigh("a") int end;

    int
                    @SameLen(
                        value = {"this", "this"},
                        offset = {"0", "this.start"}
                    )
                    []
            a;

    public SameLenOffsets(int[] a1) {
        // the following line is the expected error when dealing with custom collections
        // :: error: (assignment.type.incompatible)
        a = a1;
    }

    public int get(@IndexFor("this") int index) {
        return a[start + index];
    }

    private static @IndexOrLow("#1") int indexOf(
            int[] array, int target, @IndexOrHigh("#1") int start, @IndexOrHigh("#1") int end) {
        for (int i = start; i < end; i++) {
            if (array[i] == target) {
                return i;
            }
        }
        return -1;
    }
}
