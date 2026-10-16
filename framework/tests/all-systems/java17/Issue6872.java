// @below-java17-jdk-skip-test
// Test case for https://github.com/typetools/checker-framework/issues/6872

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.checkerframework.dataflow.qual.Pure;

public class Issue6872 {
  @Pure
  public static Integer compare(Object x, Object y) {
    return 0;
  }

  public static <T> void test(Collection<List<T>> p1, Comparator<Object> defaultComparator) {
    Collections.min(
        p1,
        switch (1) {
          case 1 -> Issue6872::compare;
          default -> defaultComparator;
        });
  }

  public static <T> void test2(
      Collection<List<T>> p1, Comparator<List<T>> c1, Comparator<Object> defaultComparator) {
    Collections.min(
        p1,
        switch (1) {
          case 1 -> c1;
          case 2 -> null;
          default -> defaultComparator;
        });
  }

  public static <T> void test3(
      boolean b, Collection<List<T>> p1, Comparator<Object> defaultComparator) {
    Collections.min(p1, b ? Issue6872::compare : defaultComparator);
  }
}
