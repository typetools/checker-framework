// Test case for Issue 1981:
// https://github.com/typetools/checker-framework/issues/1981

import java.util.List;

public class Issue1981 {

  void test(List<Integer> ids) {
    for (List<Integer> l : func2(func1(ids))) {}
  }

  static <E extends Comparable<? super E>> List<E> func1(Iterable<? extends E> elements) {
    // :: error: (return)
    return null;
  }

  static <T> List<List<T>> func2(List<T> list) {
    // :: error: (return)
    return null;
  }
}
