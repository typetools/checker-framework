import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

class SetOperations {

  void testIsEmpty(Set<Integer> nums) {
    if (nums.isEmpty()) {
      // :: error: (assignment)
      @NonEmpty Set<Integer> nums2 = nums;
    } else {
      @NonEmpty Set<Integer> nums3 = nums; // OK
    }
  }

  void testContains(Set<Integer> nums) {
    if (nums.contains(1)) {
      @NonEmpty Set<Integer> nums2 = nums;
    } else {
      // :: error: (assignment)
      @NonEmpty Set<Integer> nums3 = nums;
    }
  }

  void testAdd(Set<Integer> nums) {
    // :: error: (assignment)
    @NonEmpty Set<Integer> nums2 = nums; // No guarantee that the set is non-empty here
    if (nums.add(1)) {
      @NonEmpty Set<Integer> nums3 = nums;
    }
  }

  void testAddAllEmptySet() {
    Set<Integer> nums = new HashSet<>();
    // :: error: (assignment)
    @NonEmpty Set<Integer> nums2 = nums;
    if (nums.addAll(Set.of())) {
      // Adding an empty set will always return false, this is effectively dead code
      @NonEmpty Set<Integer> nums3 = nums;
    } else {
      // :: error: (assignment)
      @NonEmpty Set<Integer> nums3 = nums;
    }
  }

  void testRemove() {
    Set<Integer> nums = new HashSet<>();
    nums.add(1);
    @NonEmpty Set<Integer> nums2 = nums;
    nums.remove(1);

    // :: error: (assignment)
    @NonEmpty Set<Integer> nums3 = nums;
  }
}
