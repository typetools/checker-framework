package typearginfer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.tainting.qual.Untainted;

public class TaintingIssue6755 {

  private final int value;

  TaintingIssue6755(int value) {
    this.value = value;
  }

  @SuppressWarnings("return")
  @Untainted int getValue() {
    return value;
  }

  static int merge(int value, Map<Integer, List<TaintingIssue6755>> data) {
    return value;
  }

  static void test(List<TaintingIssue6755> externals) {
    var testsByValue =
        Stream.of(new TaintingIssue6755(1), new TaintingIssue6755(2))
            .collect(Collectors.groupingBy(TaintingIssue6755::getValue));
    Map<@Untainted Integer, List<TaintingIssue6755>> map = testsByValue;
    var values =
        Stream.of(1, 2)
            .map(
                // :: error: (argument)
                val -> merge(val, testsByValue));
  }
}
