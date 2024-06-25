import java.util.Iterator;
import java.util.List;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

class IteratorOperations {

  void testPolyNonEmptyIterator(List<Integer> nums) {
    // :: error: (method.invocation)
    nums.iterator().next();

    if (!nums.isEmpty()) {
      @NonEmpty Iterator<Integer> nonEmptyIterator = nums.iterator();
      nonEmptyIterator.next();
    } else {
      // :: error: (assignment)
      @NonEmpty Iterator<Integer> unknownEmptyIterator = nums.iterator();
    }
  }

  void testSwitchRefinementNoFallthrough(List<Integer> nums) {
    switch (nums.size()) {
      case 0:
        // :: error: (method.invocation)
        nums.iterator().next();
        break;
      case 1:
        @NonEmpty List<Integer> nums2 = nums; // OK
        break;
      default:
        @NonEmpty List<Integer> nums3 = nums; // OK
    }
  }

  void testSwitchRefinementWithFallthrough(List<Integer> nums) {
    switch (nums.size()) {
      case 0:
        // :: error: (method.invocation)
        nums.iterator().next();
      case 1:
        // :: error: (assignment)
        @NonEmpty List<Integer> nums2 = nums;
      default:
        // :: error: (assignment)
        @NonEmpty List<Integer> nums3 = nums;
    }
  }

  void testSwitchRefinementNoZero(List<Integer> nums) {
    switch (nums.size()) {
      case 1:
        nums.iterator().next();
        break;
      default:
        // :: error: (assignment)
        @NonEmpty List<Integer> nums3 = nums;
    }
  }

  void testSwitchRefinementIndexOf(List<String> strs, String s) {
    switch (strs.indexOf(s)) {
      case -1:
        // :: error: (method.invocation)
        strs.iterator().next();
        break;
      case 0:
        @NonEmpty List<String> strs2 = strs;
      case 2:
      case 3:
        strs.iterator().next();
        break;
      default:
        @NonEmpty List<String> strs3 = strs;
    }
  }
}
