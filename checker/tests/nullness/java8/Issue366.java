// Test case for Issue 366:
// https://github.com/typetools/checker-framework/issues/366
// but amended for Issue 1098:
// https://github.com/typetools/checker-framework/issues/1098

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

public class Issue366 {
    static Optional<@NonNull String> getPossiblyEmptyString() {
        return Optional.ofNullable(null);
    }

    static Optional<@Nullable String> getPossiblyEmptyString2() {
        return Optional.ofNullable(null);
    }
}
