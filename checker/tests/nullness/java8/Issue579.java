// Test case for Issue579
// https://github.com/typetools/checker-framework/issues/579

import java.util.Comparator;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class Issue579<T> implements Comparator<T> {
  private final Comparator<T> real;

  @SideEffectFree
  @SuppressWarnings("unchecked")
  Issue579(Comparator<? super T> real) {
    this.real = (Comparator<T>) real;
  }

  @Override
  public int compare(T a, T b) {
    throw new RuntimeException();
  }

  @Override
  public Comparator<T> thenComparing(Comparator<? super T> other) {
    // :: warning: [nulltest.redundant]
    return new Issue579<>(real == null ? other : real.thenComparing(other));
  }
}
