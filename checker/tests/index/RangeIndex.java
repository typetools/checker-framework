import org.checkerframework.common.value.qual.*;

public class RangeIndex {
    void foo(@IntRange(from = 0, to = 11) int x, int @MinLen(10) [] a) {
        // :: error: (array.access.unsafe.high.range)
        int y = a[x];
    }
}
