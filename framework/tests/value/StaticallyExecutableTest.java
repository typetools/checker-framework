// Test case for https://github.com/typetools/checker-framework/issues/1288

// @skip-test until the issue is fixed.

import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StaticallyExecutable;
import org.checkerframework.dataflow.qual.Pure;

public class StaticallyExecutableTest {

    @Pure
    @StaticallyExecutable
    public static int pow(int base, int expt) throws ArithmeticException {
        return 7;
    }

    public static final int UNMODIFIED = 0;

    public static final @Positive int UNMODIFIED_BITVAL = pow(2, UNMODIFIED);

    public static final @IntVal(7) int UNMODIFIED_BITVAL_2 = pow(2, UNMODIFIED);

    public static final @Positive int UNMODIFIED_BITVAL_3 = 7;

    public static final @IntVal(7) int UNMODIFIED_BITVAL_4 = 7;
}
