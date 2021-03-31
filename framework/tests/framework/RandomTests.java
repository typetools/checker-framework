import java.util.Collections;
import java.util.List;

public class RandomTests {
  // Test that boxing occurs even if the varable (assigned to value) is not
  // a declared type
  void testBoxing() {
    int i = 0;
    Collections.singleton(i);
  }

  void testWildcards() {
    List<?> l = null;
  }
}
