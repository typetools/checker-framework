package open.crash;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// @below-java17-jdk-skip-test
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
