// Test case that was submitted in Issue 402, but was combined with Issue 979
// https://github.com/typetools/checker-framework/issues/979

// @below-java8-jdk-skip-test

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue980 {

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
