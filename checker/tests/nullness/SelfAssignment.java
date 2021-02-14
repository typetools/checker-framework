// Test case for issue #231

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SelfAssignment {

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
