import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nonempty.qual.NonEmpty;

class UnmodifiableTest {

  void foo(@NonEmpty List<String> strs) {
    Collections.unmodifiableList(strs).iterator().next(); // OK
  }
}
