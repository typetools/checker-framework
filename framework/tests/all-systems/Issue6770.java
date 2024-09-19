package typearginfer;

import java.util.function.BiPredicate;
import java.util.function.Supplier;

public abstract class Issue6770 {

  public static <T> Supplier<T> memoize(Supplier<T> delegate) {
    return delegate;
  }

  private final Supplier<BiPredicate<Integer, String>> supplier = memoize(() -> this::accept);

  protected abstract boolean accept(Integer scope, String name);
}
