import java.util.ArrayList;
import java.util.List;

// @skip-test until JDK is annotated with Non-Empty type qualifiers

class ListOperations {

  void testGetOnEmptyList(List<String> strs) {
    // :: error: (method.invocation)
    strs.get(0);
  }

  void testGetOnNonEmptyList(List<String> strs) {
    if (strs.isEmpty()) {
      // :: error: (method.invocation)
      strs.get(0);
    } else {
      strs.get(0); // OK
    }
  }

  void testAddToEmptyListAndGet() {
    List<Integer> nums = new ArrayList<>();
    nums.add(1); // nums has type @NonEmpty after this line
    nums.get(0); // OK
  }

  // TODO: consider other sequences (e.g., calling get(int) after clear())
}
