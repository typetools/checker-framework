package inference;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public abstract class MemRefInfere<K, V> implements Map<K, V>, Serializable {
  public static <K1, V1> MemRefInfere<K1, V1> copyOf(Map<? extends K1, ? extends V1> map) {
    throw new RuntimeException();
  }

  public static <T, K2, V2> Collector<T, ?, MemRefInfere<K2, V2>> toImmutableMap(
      Function<? super T, ? extends K2> keyFunction,
      Function<? super T, ? extends V2> valueFunction,
      BinaryOperator<V2> mergeFunction) {

    return Collectors.collectingAndThen(
        Collectors.toMap(keyFunction, valueFunction, mergeFunction, LinkedHashMap::new),
        MemRefInfere::copyOf);
  }
}
