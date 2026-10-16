// Test case for https://github.com/typetools/checker-framework/issues/6716

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;
import org.checkerframework.checker.interning.qual.Interned;

public class Issue6716 {

  void method(Stream<Integer> integerStream) {
    LinkedHashMap<Integer, Integer> c =
        integerStream.collect(toMap(Function.identity(), v -> 1, Issue6716::sum));
  }

  public static <T, K, U, M extends Map<K, U>> Collector<T, ?, M> toMap(
      Function<? super T, ? extends K> keyMapper,
      Function<? super T, ? extends U> valueMapper,
      BinaryOperator<U> mergeFunction) {
    throw new RuntimeException();
  }

  static @Interned Integer sum(Integer a, Integer b) {
    return a + b;
  }
}
