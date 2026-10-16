// @below-java17-jdk-skip-test
// Test case for https://github.com/typetools/checker-framework/issues/6872

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue6872 {
  <T> void test(
      int i,
      Comparator<List<@NonNull T>> c1,
      Comparator<@Nullable Object> c2,
      Collection<List<@NonNull T>> nonNullLists,
      Collection<List<@Nullable T>> nullableLists) {
    Collections.min(
        nonNullLists,
        switch (i) {
          case 1 -> c1;
          default -> c2;
        });
    // The lub of the switch arms is Comparator<? super List<@NonNull T>>.
    // :: error: (type.arguments.not.inferred)
    Collections.min(
        nullableLists,
        switch (i) {
          case 1 -> c1;
          default -> c2;
        });
  }
}
