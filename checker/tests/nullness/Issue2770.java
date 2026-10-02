// Test case for https://github.com/typetools/checker-framework/issues/2770

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

abstract class Issue2770 {
  class Box<R> {}

  abstract <S> S unbox(Box<S> b);

  abstract <T> @NonNull T checkNotNull(@Nullable T sample);

  void test(Object s) {}

  void bar(Box<?> s) {
    test(checkNotNull(unbox(s)));

    Object o = checkNotNull(unbox(s));
    test(o);
  }
}
