// Test case for Issue 1332.
// https://github.com/typetools/checker-framework/issues/1332
// @below-java8-jdk-skip-test

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("") // check for crashes
abstract class Issue1332 {
    void foo(List<Long> ll) {
        Function<String, Long> test =
                s -> {
                    long result = getOnlyElement(ll.stream().collect(Collectors.toSet()));
                    return result;
                };
    }

    abstract <T> T getOnlyElement(Iterable<T> iterable);
}
