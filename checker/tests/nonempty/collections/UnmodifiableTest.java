import java.util.Collections;
import java.util.List;
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

  void testVarArgsEmpty() {
    // :: error: (assignment)
    @NonEmpty List<String> items = List.of();
  }

  void testVarArgsNonEmpty() {
    // Requires more than 10 elements to invoke the varargs version
    @NonEmpty List<Integer> items = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12); // OK
  }
}
