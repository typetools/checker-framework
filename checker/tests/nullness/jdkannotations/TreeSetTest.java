// Partial test case for issue #1330: https://github.com/typetools/checker-framework/issues/1330
// This should be expanded to include all the cases in the issue.

// @skip-test until we fix the issue

import java.util.TreeSet;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TreeSetTest {

  public static void main(String[] args) {

    // :: error: (type.argument)
    TreeSet<@Nullable Integer> ts = new TreeSet<>();

    // This throws a null pointer exception
    ts.add(null);
  }
}
