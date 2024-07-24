import static java.util.Map.entry;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

class UnmodifiableTest {

  void unmodifiableCopy(@NonEmpty List<String> strs) {
    @NonEmpty List<String> strsCopy = Collections.unmodifiableList(strs); // OK
  }

  void checkNonEmptyThenCopy(List<String> strs) {
    if (strs.isEmpty()) {
      // :: error: (method.invocation)
      Collections.unmodifiableList(strs).iterator().next();
    } else {
      Collections.unmodifiableList(strs).iterator().next(); // OK
    }
  }

  void testVarargsEmpty() {
    // :: error: (assignment)
    @NonEmpty List<String> items = List.of();
  }

  void testVarargsNonEmptyList() {
    // Requires more than 10 elements to invoke the varargs version
    @NonEmpty List<Integer> items = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12); // OK
  }

  void testVarargsNonEmptyMap() {
    // Requires more than 10 elements to invoke the varargs version
    @NonEmpty
    Map<String, Integer> map =
        Map.ofEntries(
            entry("a", 1),
            entry("b", 2),
            entry("c", 3),
            entry("d", 4),
            entry("e", 5),
            entry("f", 6),
            entry("g", 7),
            entry("h", 8),
            entry("i", 9),
            entry("j", 10),
            entry("k", 11),
            entry("l", 12));
  }
}
