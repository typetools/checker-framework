// Test case for issue #231

import checkers.nullness.quals.EnsuresNonNull;
import checkers.nullness.quals.Nullable;

class SelfAssignment {

    void test(@Nullable String s) {
       assertNonNull(s);
       s = s.trim();
    }

    @EnsuresNonNull("#1")
    void assertNonNull(final @Nullable Object o) {
        if (o == null) {
            throw new AssertionError();
        }
    }
}
