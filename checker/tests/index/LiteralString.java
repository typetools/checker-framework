// Test case for issue #55:
// https://github.com/kelloggm/checker-framework/issues/55

// @skip-test until the Index Checker supports strings

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.common.value.qual.MinLen;

public class LiteralString {

    private static final String[] finalField = {"This", "is", "an", "array"};

    void testLiteralString() {
        @MinLen(10) String s = "This string is long enough";
    }

    void testLiteralArray() {
        String @MinLen(2) [] a = new String[] {"This", "array", "is", "long", "enough"};
        String @MinLen(2) [] b = finalField;
        @IndexFor("finalField") int i = 0;
    }
}
