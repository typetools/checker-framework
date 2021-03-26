package inference.bug8;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;

@SuppressWarnings("all") // Just check for crashes.
public class Bug8 {

  static <T1, K1, V1> Collector<T1, ?, MyMap<K1, V1>> toImmutableMap(
      Function<? super T1, ? extends K1> keyFunction,
      Function<? super T1, ? extends V1> valueFunction) {
    return Collector.of(
        MyMap.Builder<K1, V1>::new,
        (builder, input) -> builder.put(keyFunction.apply(input), valueFunction.apply(input)),
        MyMap.Builder::combine,
        MyMap.Builder::build);
  }

  static <T, K, V> Collector<T, ?, MyBiMap<K, V>> toImmutableBiMap(
      Function<? super T, ? extends K> keyFunction,
      Function<? super T, ? extends V> valueFunction) {
    return Collector.of(
        MyBiMap.Builder<K, V>::new,
        (builder, input) -> builder.put(keyFunction.apply(input), valueFunction.apply(input)),
        MyBiMap.Builder::combine,
        MyBiMap.Builder::build,
        new Collector.Characteristics[0]);
  }

  abstract static class ShimMap<K, V> extends MyMap<K, V> {}

  public interface BiMap<K, V> extends Map<K, V> {}

  public abstract static class MyBiMap<K, V> extends ShimMap<K, V> implements BiMap<K, V> {
    public static final class Builder<K, V> extends MyMap.Builder<K, V> {
      public Builder() {}

      @Override
      public MyBiMap.Builder<K, V> put(K key, V value) {
        throw new RuntimeException();
      }

      @Override
      MyBiMap.Builder<K, V> combine(MyMap.Builder<K, V> builder) {
        super.combine(builder);
        return this;
      }

      @Override
      public MyBiMap<K, V> build() {
        throw new RuntimeException();
      }
    }
  }

  public abstract static class MyMap<K, V> implements Map<K, V>, Serializable {
    public static class Builder<K, V> {
      public Builder() {}

      public MyMap.Builder<K, V> put(K key, V value) {
        throw new RuntimeException();
      }

      MyMap.Builder<K, V> combine(MyMap.Builder<K, V> other) {
        throw new RuntimeException();
      }

      public MyMap<K, V> build() {
        throw new RuntimeException();
      }
    }
  }
}
