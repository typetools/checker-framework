// Test case for Issue 1006:
// https://github.com/typetools/checker-framework/issues/1006

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("all") // Ignore type-checking errors.
public class Issue1006 {
  void foo(Stream<String> m, Map<String, Integer> im) {
    Map<String, Integer> l = m.collect(Collectors.toMap(Function.identity(), im::get));
  }

  // alternative version with same crash
  Map<String, Long> bar(String src) {
    return Stream.of(src)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
  }
}
