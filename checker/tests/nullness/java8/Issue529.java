// Test case for issue 529:
// https://github.com/typetools/checker-framework/issues/529
// @below-java8-jdk-skip-test

import java.util.*;
import java.util.stream.*;

public class Issue529 {

  // Crashes:
  public Stream<String> test(List<String> list) {
    return list.stream().map(e -> e);
  }

  // OK:
  public Stream<String> test2(List<String> list) {
    return list.stream();
  }

  // OK:
  public Stream<String> test3(Stream<String> stream) {
    return stream.map(e -> e);
  }

}
