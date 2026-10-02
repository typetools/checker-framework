// Test case for https://github.com/typetools/checker-framework/issues/8298

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue8298Nullness {
  static class Box<T extends @Nullable Object> {}

  static <T extends @Nullable Object> Box<? extends T> wildcardBox(Box<T> b) {
    throw new RuntimeException();
  }

  static <X extends @Nullable Object> X takeExtends(Box<? extends X> x) {
    throw new RuntimeException();
  }

  static <U extends @Nullable Object> void nullableBound(Box<U> box) {
    // :: error: (dereference.of.nullable)
    takeExtends(wildcardBox(box)).toString();
    U u = takeExtends(wildcardBox(box));
  }

  static <U extends @Nullable Object> void nonNullUse(Box<@NonNull U> box) {
    takeExtends(wildcardBox(box)).toString();
    @NonNull U u = takeExtends(wildcardBox(box));
  }

  static <U extends @Nullable Object> void nullableUse(Box<@Nullable U> box) {
    // :: error: (argument)
    @NonNull Object o = takeExtends(wildcardBox(box));
  }

  static <U extends Comparable<U>> void fBounded(Box<U> box, U other) {
    takeExtends(wildcardBox(box)).compareTo(other);
  }

  static void string(Box<@Nullable String> box) {
    // :: error: (dereference.of.nullable)
    takeExtends(wildcardBox(box)).length();
    @Nullable String s = takeExtends(wildcardBox(box));
  }
}
