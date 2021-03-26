// Test case for Issue 369:
// https://github.com/typetools/checker-framework/issues/369

import static java.util.stream.Collectors.toSet;

import java.util.stream.Stream;

public class Issue369 {
  static void test(Stream<Integer> stream) {
    stream.collect(toSet());
  }
}
