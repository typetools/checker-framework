// Test case for Issue 1334
// https://github.com/typetools/checker-framework/issues/1334
// @below-java8-jdk-skip-test

import java.util.stream.Stream;

public class Issue1334 {
    private void test() {
        Stream<Integer> stream = Stream.of(new byte[][] {}).map(b -> 1);
    }
}
