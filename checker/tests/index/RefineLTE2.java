// Test case for issue #62:
// https://github.com/kelloggm/checker-framework/issues/62

import org.checkerframework.checker.index.qual.LTEqLengthOf;
import org.checkerframework.common.value.qual.MinLen;

@SuppressWarnings("lowerbound")
public class RefineLTE2 {

    protected int @MinLen(1) [] values;

    @LTEqLengthOf("values") int num_values;

    public void add(int elt) {
        if (num_values == values.length) {
            values = null;
            // :: error: (unary.increment.type.incompatible)
            num_values++;
            return;
        }
        values[num_values] = elt;
        num_values++;
    }
}
