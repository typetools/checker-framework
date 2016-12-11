// Test case for issue #14:
// https://github.com/kelloggm/checker-framework/issues/14

// @skip-test until the bug is fixed

import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.checker.upperbound.qual.*;
import org.checkerframework.common.value.qual.*;

public class ArrayLength3 {
    String getFirst(String /*@ArrayLen(2)*/[] sa) {
        return sa[0];
    }

    void m() {
        Integer[] a = new Integer[10];
        @LTLengthOf("a") int i = 5;
    }
}
