import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.MinLen;

// test case for kelloggm#183: https://github.com/kelloggm/checker-framework/issues/183

public class UncheckedMinLen {
    void addToUnboundedIntRange(@IntRange(from = 0) int l, Object v) {
        // :: error: (assignment.type.incompatible)
        Object @MinLen(100) [] o = new Object[l + 1];
        o[99] = v;
    }

    void addToBoundedIntRangeOK(@IntRange(from = 0, to = 1) int l, Object v) {
        // :: error: (assignment.type.incompatible)
        Object @MinLen(100) [] o = new Object[l + 1];
        o[99] = v;
    }
}
