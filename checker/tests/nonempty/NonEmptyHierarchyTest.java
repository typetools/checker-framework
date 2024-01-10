import java.util.List;
import org.checkerframework.checker.nonempty.qual.NonEmpty;
import org.checkerframework.checker.nonempty.qual.UnknownNonEmpty;

class NonEmptyHierarchyTest {

  void testAssignments(@NonEmpty List<String> l1, @UnknownNonEmpty List<String> l2) {
    @NonEmpty List<String> l3 = l1; // OK, both are @NonEmpty

    // :: error: (assignment)
    @NonEmpty List<String> l4 = l2; // Error for this line
  }
}
