import java.util.Map;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

// @skip-test until JDK is annotated with Non-Empty type qualifiers

class ImmutableMapOperations {

  void emptyImmutableMap() {
    Map<String, Integer> emptyMap = Map.of();
    // :: error: (assignment)
    @NonEmpty Map<String, Integer> nonEmptyMap = emptyMap;
  }

  void nonEmptyImmutableMap() {
    Map<String, Integer> nonEmptyMap = Map.of("Hello", 1);
    @NonEmpty Map<String, Integer> m1 = nonEmptyMap;
  }

  void immutableCopyEmptyMap() {
    Map<String, Integer> emptyMap = Map.of();
    // :: error: (assignment)
    @NonEmpty Map<String, Integer> nonEmptyMap = Map.copyOf(emptyMap);
  }

  void immutableCopyNonEmptyMap() {
    Map<String, Integer> nonEmptyMap = Map.of("Hello", 1, "World", 2);
    @NonEmpty Map<String, Integer> m2 = Map.copyOf(nonEmptyMap);
  }
}
