package inference;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public abstract class MemRefInfere<K, V> implements Map<K, V>, Serializable {
    public static <K, V> MemRefInfere<K, V> copyOf(Map<? extends K, ? extends V> map) {
        throw new RuntimeException();
    }

    public static <T, K, V> Collector<T, ?, MemRefInfere<K, V>> toImmutableMap(
            Function<? super T, ? extends K> keyFunction,
            Function<? super T, ? extends V> valueFunction,
            BinaryOperator<V> mergeFunction) {

        return Collectors.collectingAndThen(
                Collectors.toMap(keyFunction, valueFunction, mergeFunction, LinkedHashMap::new),
                MemRefInfere::copyOf);
    }
}
