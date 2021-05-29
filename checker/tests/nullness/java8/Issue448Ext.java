// Test case for issue 448:
// https://github.com/typetools/checker-framework/issues/448

import java.util.Arrays;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class Issue448Ext {
  void getFor(int[] ia, int index) {
    Arrays.stream(ia).filter(x -> true);
  }

  Object getFor(int[] ia, IntPredicate p) {
    return Arrays.stream(ia).filter(p);
  }

  Object getFor(IntStream is, int index) {
    return is.filter(key -> key == index).findFirst().orElseThrow(IllegalArgumentException::new);
  }
}
