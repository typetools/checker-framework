// Test case for Issue 306:
// https://github.com/typetools/checker-framework/issues/306

// @skip-test until the issue is fixed

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue306 {
    @MonotonicNonNull Object x;

    static <T> T check(T var) {
        return var;
    }

    void fakeMethod() {
        // @MonotonicNonNull is not reflexive.
        // However, it is the most specific type argument
        // inferred for check. Therefore, an error is
        // raised.
        // We need a mechanism to not consider a
        // qualifier in type inference.
        check(x);

        // Ugly way around the problem:
        Issue306.<@Nullable Object>check(x);

        // The following error has to be raised: from
        // the signature it is not guaranteed that
        // the parameter is returned.
        // :: error: (monotonic.type.incompatible)
        x = check(x);
    }

    @MonotonicNonNull Object y;

    void realError(@MonotonicNonNull Object p) {
        // :: error: (monotonic.type.incompatible)
        x = y;
        // :: error: (monotonic.type.incompatible)
        x = p;
        // It would be nice not to raise the following
        // error.
        // :: error: (monotonic.type.incompatible)
        x = x;
    }
}
