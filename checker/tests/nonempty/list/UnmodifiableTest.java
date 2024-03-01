import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nonempty.qual.NonEmpty;
import org.checkerframework.checker.nonempty.qual.PolyNonEmpty;

class UnmodifiableTest {

  void foo(@NonEmpty List<String> strs) {
    @NonEmpty List<String> copy = Collections.unmodifiableList(strs);

    @NonEmpty List<String> copy2 = unmodifiableList(strs); // WORKS!

    Collections.unmodifiableList(strs).iterator().next(); // should be OK
  }

  static <T> @PolyNonEmpty List<T> unmodifiableList(@PolyNonEmpty List<? extends T> list) {
    return null;
  }
}
