// Test case for issue #66:
// https://github.com/kelloggm/checker-framework/issues/66

// @skip-test until the issue is fixed

import org.checkerframework.common.value.qual.MinLen;

public class ArrayConstructionPositiveLength {

    public void makeArray(/*@Positive*/ int max_values) {
        String @MinLen(1) [] a = new String[max_values];
    }
}
