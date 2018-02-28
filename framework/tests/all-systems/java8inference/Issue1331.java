// Test case for Issue 1331.
// https://github.com/typetools/checker-framework/issues/1331

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("") // check for crashes
class Issue1331 {
    List<Long> ll;
    long result = getOnlyElement(ll.stream().collect(Collectors.toSet()));

    static <T> T getOnlyElement(Iterable<T> iterable) {
        return null;
    }
}
