package inference.guava;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

@SuppressWarnings("") // Just check for crashes.
public class Bug7 {
    static <T, K, V> Collector<T, ?, MyMap<K, V>> toMap(
            Function<? super T, ? extends K> keyFunction,
            Function<? super T, ? extends V> valueFunction) {
        return Collector.of(
                MyMap.Builder<K, V>::new,
                (builder, input) ->
                        builder.put(keyFunction.apply(input), valueFunction.apply(input)),
                MyMap.Builder::combine,
                MyMap.Builder::build);
    }

    public abstract static class MyMap<K, V> implements Map<K, V>, Serializable {
        public static class Builder<K, V> {

            public Builder() {}

            public Builder<K, V> put(K key, V value) {
                throw new RuntimeException();
            }

            Builder<K, V> combine(Builder<K, V> other) {
                throw new RuntimeException();
            }

            public MyMap<K, V> build() {
                throw new RuntimeException();
            }
        }
    }
}
