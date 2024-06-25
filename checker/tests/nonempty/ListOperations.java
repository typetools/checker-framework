import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

class ListOperations {

  // Skip test until we decide whether to handle accesses on empty containers
  // void testGetOnEmptyList(List<String> strs) {
  //   // :: error: (method.invocation)
  //   strs.get(0);
  // }

  // Skip test until we decide whether to handle accesses on empty containers
  // void testGetOnNonEmptyList(List<String> strs) {
  //   if (strs.isEmpty()) {
  //     // :: error: (method.invocation)
  //     strs.get(0);
  //   } else {
  //     strs.get(0); // OK
  //   }
  // }

  void testAddToEmptyListAndGet() {
    List<Integer> nums = new ArrayList<>();
    nums.add(1); // nums has type @NonEmpty after this line
    nums.get(0); // OK
  }

  void testAddAllWithEmptyList() {
    List<Integer> nums = new ArrayList<>();
    nums.addAll(List.of());
    // :: error: (assignment)
    @NonEmpty List<Integer> nums2 = nums;
  }

  void testAddAllWithNonEmptyList() {
    List<Integer> nums = new ArrayList<>();
    if (nums.addAll(List.of(1, 2, 3))) {
      @NonEmpty List<Integer> nums2 = nums; // OK
    }
  }

  void testContains(List<Integer> nums) {
    if (nums.contains(11)) {
      @NonEmpty List<Integer> nums2 = nums; // OK
    }
    // :: error: (assignment)
    @NonEmpty List<Integer> nums2 = nums;
  }
  // TODO: consider other sequences (e.g., calling get(int) after clear())
}
