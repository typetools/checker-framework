// Test case for issue #56:
// https://github.com/kelloggm/checker-framework/issues/56

// @skip-test until the issue is fixed

import org.checkerframework.checker.index.qual.MinLen;

public class EndsWith {

    void testEndsWith(String arg) {
        if (arg.endsWith("[]")) {
            @MinLen(2) String arg2 = arg;
        }
    }
}
