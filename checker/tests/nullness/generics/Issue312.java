// Test case for Issue 312:
// https://github.com/typetools/checker-framework/issues/312

import java.util.List;

// A mock-up of the Guava API used in the test case;
// see below for the code that uses Guava.
@SuppressWarnings("nullness")
class Ordering312<T> {
  public static <C extends Comparable> Ordering312<C> natural() {
    return null;
  }

  public <S extends T> Ordering312<S> reverse() {
    return null;
  }

  public <E extends T> List<E> sortedCopy(Iterable<E> elements) {
    return null;
  }
}

public class Issue312 {
  void test(List<String> list) {
    Ordering312.natural().reverse().sortedCopy(list);
  }
}

/* Original test using Guava:

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering312;
import java.util.List;

public class Issue312 {
    void test() {
        List<String> list = Lists.newArrayList();
        Ordering.natural().reverse().sortedCopy(list);
    }
}

*/
