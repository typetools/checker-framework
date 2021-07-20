// Test case for issue #979:
// https://github.com/typetools/checker-framework/issues/979

// @skip-test until the bug is fixed

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectorsToList {

    void m(List<String> strings) {
        Stream<String> s = strings.stream();

        // This works:
        List<String> collectedStrings1 = s.collect(Collectors.<String>toList());
        // This works:
        List<@Nullable String> collectedStrings2 = s.collect(Collectors.toList());
        // This works:
        @SuppressWarnings("nullness")
        List<String> collectedStrings3 = s.collect(Collectors.toList());

        // This assignment issues a warning due to incompatible types:
        List<String> collectedStrings = s.collect(Collectors.toList());

        collectedStrings.forEach(System.out::println);
    }
}
