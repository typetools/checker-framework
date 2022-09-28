// Test case that was submitted in Issue 402, but was combined with Issue 979
// https://github.com/typetools/checker-framework/issues/979

// @above-java17-jdk-skip-test TODO: reinstate, false positives may be due to issue #979

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

    List<String> collectedStrings = s.collect(Collectors.toList());

    collectedStrings.forEach(System.out::println);
  }
}
