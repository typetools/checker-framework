// Test case for Issue 366:
// https://code.google.com/p/checker-framework/issues/detail?id=366

// @skip-test
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.NonNull;


class Test {
    static Optional<@NonNull String> getPossiblyEmptyString() {
        return Optional.ofNullable(null);
    }

    static Optional<@Nullable String> getPossiblyEmptyString2() {
        // The optional returned is still @NonNull.
        //:: error: (return.type.incompatible)
        return Optional.ofNullable(null);
    }
}