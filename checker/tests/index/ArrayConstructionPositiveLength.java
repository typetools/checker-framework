// Test case for issue #66:
// https://github.com/kelloggm/checker-framework/issues/66

import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.*;

public class ArrayConstructionPositiveLength {

    public void makeArray(@Positive int max_values) {
        String @MinLen(1) [] a = new String[max_values];
    }
}
