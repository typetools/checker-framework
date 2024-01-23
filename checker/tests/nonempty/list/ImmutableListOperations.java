import java.util.List;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

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
