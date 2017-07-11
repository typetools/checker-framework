// Test case for issue #1345:
// https://github.com/typetools/checker-framework/issues/1345
// @below-java8-jdk-skip-test

// @skip-test until the issue is resolved

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.Opt;
import org.checkerframework.checker.nullness.qual.*;

public class Issue1345 {

    @EnsuresNonNullIf(expression = "#1", result = true)
    static boolean isNonNull(@Nullable Object o) {
        return o != null;
    }

    void filterPresent_Optional(Stream<@Nullable BigDecimal> s) {
        Stream<@NonNull BigDecimal> filtered = s.<BigDecimal>filter(Issue1345::isNonNull);
    }

    void filterPresent_Opt(@Nullable Object p) {
        @NonNull Object o = Opt.filter(p, Opt::isPresent);
    }
}
