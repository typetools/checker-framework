// Test case for issue #55:
// https://github.com/kelloggm/checker-framework/issues/55

// @skip-test until the issue is fixed

import org.checkerframework.checker.minlen.qual.MinLen;

public class LiteralString {

    void testLiteralString() {
        @MinLen(10) String s = "This string is long enough";
        String @MinLen(2) [] a = new String[] {"This", "array", "is", "long", "enough"};
    }
}
