// Test case for https://github.com/typetools/checker-framework/issues/6881

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue6881 {

  public static void main(String[] args) {
    List<List<? extends Number>> lists = new ArrayList<>();
    List<? super List<? extends Number>> y = lists;
    // This assignment is not null-safe.
    // :: error: (assignment)
    List<? super @Nullable List<? extends Number>> x = y;
    x.add(null);
    lists.get(0).toString();
  }
}
