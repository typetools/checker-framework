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
}
