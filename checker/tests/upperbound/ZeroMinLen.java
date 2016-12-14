// Test case for issue #57:
// https://github.com/kelloggm/checker-framework/issues/57

// @skip-test until the issue is fixed

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.minlen.qual.MinLen;

public class ZeroMinLen {

    int @MinLen(1) [] nums;

    @IndexFor("nums") int current_index;

    void test() {
        current_index = 0;
    }
}
