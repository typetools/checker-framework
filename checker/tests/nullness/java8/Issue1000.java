// Test case for issue #1000:
// https://github.com/typetools/checker-framework/issues/1000

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

public class Issue1000 {
    void illegalInstantiation(Optional<@Nullable String> arg) {}

    String orElseAppliedToNonNull(Optional<String> opt) {
        return opt.orElse("");
    }

    String orElseAppliedToNullable(Optional<String> opt) {
        // :: error: (return.type.incompatible)
        return opt.orElse(null);
    }
}
