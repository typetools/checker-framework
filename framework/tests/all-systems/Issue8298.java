// Test case for https://github.com/typetools/checker-framework/issues/8298

import java.util.List;

public class Issue8298 {
  static class Box<T> {}

  static <T> Box<? extends T> wildcardBox(Box<T> b) {
    throw new RuntimeException();
  }

  static <X> X takeExtends(Box<? extends X> x) {
    throw new RuntimeException();
  }

  static <X> X takeList(Box<? extends List<X>> x) {
    throw new RuntimeException();
  }

  static <U> void m(Box<U> box) {
    takeExtends(wildcardBox(box));
  }

  static <U extends Comparable<U>> void fBounded(Box<U> box, U other) {
    takeExtends(wildcardBox(box)).compareTo(other);
  }

  static void string(Box<String> box) {
    takeExtends(wildcardBox(box)).length();
  }

  static void list(Box<List<String>> box) {
    String s = takeList(wildcardBox(box));
  }
}
