import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("all") // Just check for crashes.
public class GuavaCrash {
  static class ImmutableMap<K, V> {
    public static <K, V> ImmutableMap<K, V> copyOf(Map<? extends K, ? extends V> map) {
      throw new RuntimeException();
    }
  }

  public static <A extends @Nullable Object, B, R>
      Collector<A, ?, ImmutableMap<B, R>> toImmutableMap(
          Function<? super A, ? extends B> keyFunction,
          Function<? super A, ? extends R> valueFunction,
          BinaryOperator<R> mergeFunction) {
    Function<Map<B, R>, ImmutableMap<? extends B, ? extends R>> finisher = ImmutableMap::copyOf;
    Supplier<LinkedHashMap<B, R>> mapFactory = LinkedHashMap::new;
    return collectingAndThen(
        toMap(keyFunction, valueFunction, mergeFunction, LinkedHashMap::new), ImmutableMap::copyOf);
  }

  public static <E, K, U, M extends Map<K, U>> Collector<E, ?, M> toMap(
      Function<? super E, ? extends K> keyMapper,
      Function<? super E, ? extends U> valueMapper,
      BinaryOperator<U> mergeFunction,
      Supplier<M> mapFactory) {
    throw new RuntimeException();
  }

  public static <T, A, Z, RR> Collector<T, A, RR> collectingAndThen(
      Collector<T, A, Z> downstream, Function<Z, RR> finisher) {
    throw new RuntimeException();
  }

  static class Table<A, B, C> {}

  private static <
          R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
      void mergeTables(
          Table<R, C, V> table, R row, C column, V value, BinaryOperator<V> mergeFunction) {
    throw new RuntimeException();
  }

  static <
          T extends @Nullable Object,
          R extends @Nullable Object,
          C extends @Nullable Object,
          V extends @Nullable Object,
          I extends Table<R, C, V>>
      Collector<T, ?, I> toTable(
          java.util.function.Function<? super T, ? extends R> rowFunction,
          java.util.function.Function<? super T, ? extends C> columnFunction,
          java.util.function.Function<? super T, ? extends V> valueFunction,
          BinaryOperator<V> mergeFunction,
          java.util.function.Supplier<I> tableSupplier) {

    return Collector.of(
        tableSupplier,
        (table, input) ->
            mergeTables(
                table,
                rowFunction.apply(input),
                columnFunction.apply(input),
                valueFunction.apply(input),
                mergeFunction),
        (table1, table2) -> {
          //          for (Table.Cell<R, C, V> cell2 : table2.cellSet()) {
          //            mergeTables(
          //                table1, cell2.getRowKey(), cell2.getColumnKey(), cell2.getValue(),
          // mergeFunction);
          //          }
          return table1;
        });
  }

  void subcrash(Method method, Class<?> p) {

    checkArgument(
        !p.isPrimitive(),
        "@Subscribe method %s's parameter is %s. "
            + "Subscriber methods cannot accept primitives. "
            + "Consider changing the parameter to %s.",
        method,
        p.getName(),
        wrap(p).getSimpleName());
  }

  void crash(Method method) {
    Class<?>[] parameterTypes = method.getParameterTypes();
    checkArgument(
        !parameterTypes[0].isPrimitive(),
        "@Subscribe method %s's parameter is %s. "
            + "Subscriber methods cannot accept primitives. "
            + "Consider changing the parameter to %s.",
        method,
        parameterTypes[0].getName(),
        wrap(parameterTypes[0]).getSimpleName());
  }

  public static <T> Class<T> wrap(Class<T> type) {
    throw new RuntimeException();
  }

  public static void checkArgument(
      boolean expression, String errorMessageTemplate, @Nullable Object... errorMessageArgs) {}

  public static <K extends @Nullable Object, V extends @Nullable Object> void difference(
      SortedMap<K, ? extends V> left, Map<? extends K, ? extends V> right) {
    Comparator<? super K> comparator = orNaturalOrder(left.comparator());
  }

  static <E extends @Nullable Object> Comparator<? super E> orNaturalOrder(
      Comparator<? super E> comparator) {
    throw new RuntimeException();
  }
}
