import org.checkerframework.checker.nullness.qual.Nullable;

public class NoStubSecond {
  public static void one(
      NoStubFirst.Supplier<Integer> supplier, NoStubFirst.Callable<@Nullable Object> callable) {
    NoStubFirst.method(supplier, callable);
  }

  public static void two(
      NoStubFirst.Supplier<Integer> supplier, NoStubFirst.Callable<Object> callable) {
    NoStubFirst.method(supplier, callable);
  }
}
