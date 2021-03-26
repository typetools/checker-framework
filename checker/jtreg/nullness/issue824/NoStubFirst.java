import org.checkerframework.checker.nullness.qual.Nullable;

public class NoStubFirst {
  public static <T extends @Nullable Object> void method(
      Supplier<T> supplier, Callable<? super T> callable) {}

  public interface Supplier<T extends @Nullable Object> {}

  public interface Callable<T extends @Nullable Object> {}
}
