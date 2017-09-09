// Test case for issue #1000:
// https://github.com/typetools/checker-framework/issues/1000
// @below-java8-jdk-skip-test

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

class Issue1000 {
    void illegalInstantiation(Optional<@Nullable String> arg) {}

    String orElseAppliedToNonNull(Optional<String> opt) {
        return opt.orElse("");
    }

    String orElseAppliedToNullable(Optional<String> opt) {
        //:: error: (return.type.incompatible)
        return opt.orElse(null);
    }
}
