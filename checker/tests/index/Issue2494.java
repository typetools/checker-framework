// Test case for issue #2494: http://tinyurl.com/cfissue/2494

import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.common.value.qual.MinLen;

public final class Issue2494 {

    static final long @MinLen(1) [] factorials = {
        1L,
        1L,
        1L * 2,
        1L * 2 * 3,
        1L * 2 * 3 * 4,
        1L * 2 * 3 * 4 * 5,
        1L * 2 * 3 * 4 * 5 * 6,
        1L * 2 * 3 * 4 * 5 * 6 * 7
    };

    static void binomialA(
            @NonNegative @LTLengthOf("Issue2494.factorials") int n,
            @NonNegative @LessThan("#1 + 1") int k) {
        @IndexFor("factorials") int j = k;
    }
}
