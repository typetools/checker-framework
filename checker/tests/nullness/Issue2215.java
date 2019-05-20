// Test Case for Issue2215
// https://github.com/typetools/checker-framework/issues/2215

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue2215 {

    @MonotonicNonNull Object f;

    Issue2215(@Nullable Object o) {

        this.f = o; // should be allowed

        this.f = null; // should be allowed
    }
}
