package typearginfer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"interning", "lock"})
public class Issue6755 {

  private final int value;

  Issue6755(int value) {
    this.value = value;
  }

  int getValue() {
    return value;
  }

  static int merge(int value, Map<Integer, List<Issue6755>> data) {
    return value;
  }

  static void test(List<Issue6755> externals) {
    var testsByValue =
        Stream.of(new Issue6755(1), new Issue6755(2))
            .collect(Collectors.groupingBy(Issue6755::getValue));
    var values = Stream.of(1, 2).map(val -> merge(val, testsByValue));
  }
}
