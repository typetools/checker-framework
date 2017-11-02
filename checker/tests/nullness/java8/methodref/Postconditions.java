// Test case for Issue #801
// https://github.com/typetools/checker-framework/issues/801
// @skip-test
// TODO: Enable post condition override checks for method references

import org.checkerframework.checker.nullness.qual.*;

interface AssertFunc {
    @EnsuresNonNullIf(result = true, expression = "#1")
    boolean testParam(final @Nullable Object param);
}

interface AssertFunc2 {
    @EnsuresNonNullIf(result = true, expression = "#1")
    boolean testParam(final @Nullable Object param);
}

class AssertionTest {
    @EnsuresNonNullIf(result = true, expression = "#1")
    static boolean override(final @Nullable Object param) {
        return param != null;
    }

    static boolean overrideAssertFunc2(final @Nullable Object param) {
        return param != null;
    }

    void context() {
        AssertFunc f = AssertionTest::override;
        // :: error: (methodref.receiver.postcondition)
        AssertFunc2 f2 = AssertionTest::overrideAssertFunc2;
    }
}
