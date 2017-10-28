// Test case for issue #68:
// https://github.com/kelloggm/checker-framework/issues/68

import org.checkerframework.checker.index.qual.IndexOrHigh;

public class TestAgainstLength {

    protected final int[] values;
    /** The number of active elements (equivalently, the first unused index). */
    @IndexOrHigh("values") int num_values;

    public TestAgainstLength() {
        values = new int[0];
    }

    public void add(int elt) {
        if (num_values == values.length) {
            return;
        }
        values[num_values] = elt;
        num_values++;
    }
}
