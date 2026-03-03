package open.crash;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// @below-java17-jdk-skip-test
// @infer-jaifs-skip-test The AFU's JAIF reading/writing libraries don't support records.
// @infer-stubs-skip-test This test outputs a warning about records.

@SuppressWarnings("all")
public class Issue7029 {

  private record Item(String id) {}

  Map<String, Item> test(Collection<Item> source) {
    return source.stream()
        .flatMap(
            item -> {
              var id = item.id();
              return (id == null) ? Stream.empty() : Stream.of(Map.entry(id, item));
            })
        .collect(Collectors.toUnmodifiableMap(entry -> entry.getKey(), entry -> entry.getValue()));
  }
}
