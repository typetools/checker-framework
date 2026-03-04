import java.util.*;
import org.checkerframework.checker.nullness.qual.*;

public class Test6872 {
  public static Integer compare(Object x, Object y) {
    return 0;
  }

  public static <T> void test(Collection<List<T>> p1, Comparator<Object> defaultComparator)
      throws Exception {
    Collections.min(
        p1,
        switch (1) {
          case 1 -> Test6872::compare;
          default -> defaultComparator;
        });
  }
}
