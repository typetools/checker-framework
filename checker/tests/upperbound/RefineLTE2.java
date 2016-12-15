// Test case for issue #62:
// https://github.com/kelloggm/checker-framework/issues/62

import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.checker.upperbound.qual.*;

public class RefineLTE2 {

    protected int @MinLen(1) [] values;

    @LTEqLengthOf("values") int num_values;

    public void add(int elt) {
        if (num_values == values.length) {
            values = null;
            num_values++;
            return;
        }
        values[num_values] = elt;
        num_values++;
    }
}
