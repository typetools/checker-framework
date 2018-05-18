// This class is a test case for the custom collections in Guava with a start and end variable and a backing array.

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.SameLen;

class SameLenOffsets {

    @NonNegative int start;

    int @SameLen(value = "this", offset = "this.start") [] a;

    public SameLenOffsets(int[] a1, @NonNegative int start1) {
        // the following line is the expected error
        // :: error: (assignment.type.incompatible)
        a = a1;
        start = start1;
    }

    public int get(@IndexFor("this") int index) {
        return a[start + index];
    }
}
