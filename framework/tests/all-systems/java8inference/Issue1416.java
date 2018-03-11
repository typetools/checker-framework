// Test case for Issue 1416.
// https://github.com/typetools/checker-framework/issues/1416

import java.util.Comparator;
import java.util.stream.Stream;

class Issue1416 {
    @SuppressWarnings("signedness")
    long order(Stream<Long> sl) {
        return sl.max(Comparator.naturalOrder()).orElse(0L);
    }
}
