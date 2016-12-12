// Test case for issue #68:
// https://github.com/kelloggm/checker-framework/issues/68

// @skip-test until the issue is fixed

import org.checkerframework.checker.index.qual.IndexOrHigh;

public class TestAgainstLength {

    protected int[] values;
    /** The number of active elements (equivalently, the first unused index). */
    @IndexOrHigh("values") int num_values;

    public void add(int elt) {
        if (num_values == values.length) {
            return;
        }
        values[num_values] = elt;
        num_values++;
    }
}
