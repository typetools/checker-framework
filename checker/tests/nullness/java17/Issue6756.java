// @below-java17-jdk-skip-test
// Test case for https://github.com/typetools/checker-framework/issues/6756

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class Issue6756 {

  abstract <T> @NonNull T get(Class<T> type);

  Collection<Map.Entry<String, String>> getEndpointMap(Stream<Issue6756> tests) {
    return tests.map(val -> Map.entry(val.get(String.class), val.get(String.class))).toList();
  }
}
