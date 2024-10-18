import java.util.List;
import org.checkerframework.checker.optional.qual.NonEmpty;

// @skip-test: these tests should not be run until a standalone Non-Empty Checker is available

class ImmutableListOperations {

  void testCreateEmptyImmutableList() {
    List<Integer> emptyInts = List.of();
    // Creating a copy of an empty list should also yield an empty list
    // :: error: (assignment)
    @NonEmpty List<Integer> copyOfEmptyInts = List.copyOf(emptyInts);
  }

  void testCreateNonEmptyImmutableList() {
    List<Integer> nonEmptyInts = List.of(1, 2, 3);
    // Creating a copy of a non-empty list should also yield a non-empty list
    @NonEmpty List<Integer> copyOfNonEmptyInts = List.copyOf(nonEmptyInts); // OK
  }
}
