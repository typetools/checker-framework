import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;
import org.checkerframework.checker.nullness.qual.KeyForBottom;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(locations = TypeUseLocation.LOWER_BOUND, value = KeyForBottom.class)
class Issue3764 {
  static <T, R, A> void use(@Nullable Collector<? super T, A, R> collector) {}

  static <T, K, V> @Nullable Collector<T, ?, Map<K, V>> calc(
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction,
      BiFunction<V, V, V> mergeFunction) {
    return null;
  }

  void foo(Function<Long, Float> f) {
    // No error when using a lambda
    use(calc(f, f, (a, b) -> Math.max(a, b)));
    // Error when using a method reference
    use(calc(f, f, Math::max));
  }
}
