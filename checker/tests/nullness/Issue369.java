// Test case for Issue 369:
// https://code.google.com/p/checker-framework/issues/detail?id=369

// @skip-test
import static java.util.stream.Collectors.toSet;

import java.util.stream.Stream;

class Test {
    static void test(Stream<Integer> stream) {
        stream.collect(toSet());
    }
}
