// Test case for Issue 369:
// https://github.com/typetools/checker-framework/issues/369

// @skip-test
import static java.util.stream.Collectors.toSet;

import java.util.stream.Stream;

// @below-java8-jdk-skip-test
class Test {
    static void test(Stream<Integer> stream) {
        stream.collect(toSet());
    }
}
