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
}
