import org.checkerframework.checker.nullness.qual.PolyNull;

public class Issue3850 {

  private static Iterable<@PolyNull String> toPos(Iterable<? extends @PolyNull Object> nodes) {
    // :: error: (return.type.incompatible)
    return transform(nodes, node -> node == null ? null : node.toString());
  }

  public static <F, T> Iterable<T> transform(
      Iterable<? extends F> iterable,
      java.util.function.Function<? super F, ? extends T> function) {
    throw new Error("implementation is irrelevant");
  }
}
