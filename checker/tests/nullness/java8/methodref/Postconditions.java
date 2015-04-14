
// @skip-test
// TODO: Enable post condition override checks for method references

import org.checkerframework.checker.nullness.qual.*;

interface AssertFunc {
    @EnsuresNonNullIf(result=true, expression="#1")
    boolean testParam(final @Nullable Object param);
}

class AssertionTest {
    @EnsuresNonNullIf(result=true, expression="#1")
    static boolean override(final @Nullable Object param) {
        return param!=null;
    }

    void context() {
        AssertFunc f = AssertionTest::override;
    }
}
