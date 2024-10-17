import java.util.List;
import org.checkerframework.checker.optional.qual.NonEmpty;
import org.checkerframework.checker.optional.qual.UnknownNonEmpty;

// @skip-test: these tests should not be run until a standalone Non-Empty Checker is available

class NonEmptyHierarchyTest {

  void testAssignments(@NonEmpty List<String> l1, @UnknownNonEmpty List<String> l2) {
    @NonEmpty List<String> l3 = l1; // OK, both are @NonEmpty

    // :: error: (assignment)
    @NonEmpty List<String> l4 = l2;

    List<String> l5 = l1; // l5 implicitly has type @UnknownNonEmpty, assigning l1 to it is legal
  }
}
