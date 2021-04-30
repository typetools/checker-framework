// Additional Test case for Issue579
// https://github.com/typetools/checker-framework/issues/579

import org.checkerframework.checker.nullness.qual.NonNull;

public class Issue579Error {

  public <T> void foo(Generic<T> real, Generic<? super T> other, boolean flag) {
    // :: error: (type.argument)
    bar(flag ? real : other);
  }

  <@NonNull Q extends @NonNull Object> void bar(Generic<? extends Q> parm) {}

  interface Generic<F> {}
}
