import org.checkerframework.checker.nullness.qual.Nullable;

public class Second {
  public static void one(First.Supplier<Integer> supplier, First.Callable<@Nullable Object> callable) {
    First.method(supplier, callable);
  }

  public static void two(First.Supplier<Integer> supplier, First.Callable<Object> callable) {
    First.method(supplier, callable);
  }
}
