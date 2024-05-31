import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue6635 {
  @FunctionalInterface
  public interface ThrowingRunnable<X extends Throwable> {
    void run() throws X;
  }

  @FunctionalInterface
  public interface ThrowingConsumer<T, X extends Throwable> {
    void accept(T t) throws X;
  }

  public static ThrowingRunnable<Exception> closing(@Nullable AutoCloseable resource) {
    return disposing(resource, AutoCloseable::close);
  }

  public static <T extends @NonNull Object, X extends Throwable> ThrowingRunnable<X> disposing(
      @Nullable T resource, ThrowingConsumer<? super T, X> disposer) {
    return () -> {
      if (resource != null) {
        disposer.accept(resource);
      }
    };
  }
}
