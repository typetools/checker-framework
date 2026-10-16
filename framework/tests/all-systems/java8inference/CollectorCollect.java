import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings("all") // just check for crashes.
public class CollectorCollect {

  static <T extends @Nullable Object, A, B>
      Collector<T, ?, ImmutableListMultimap<A, B>> flatteningToImmutableListMultimap(
          Function<? super T, ? extends A> keyFunction,
          Function<? super T, ? extends Stream<? extends B>> valuesFunction) {
    return Collectors.collectingAndThen(
        flatteningToMultimap(
            input -> checkNotNull(keyFunction.apply(input)),
            input -> valuesFunction.apply(input).peek(CollectorCollect::checkNotNull),
            MultimapBuilder.linkedHashKeys().arrayListValues()::<A, B>build),
        ImmutableListMultimap::copyOf);
  }

  static <
          T extends @Nullable Object,
          K extends @Nullable Object,
          V extends @Nullable Object,
          M extends MultimapBuilder.Multimap<K, V>>
      Collector<T, ?, M> flatteningToMultimap(
          Function<? super T, ? extends K> keyFunction,
          Function<? super T, ? extends Stream<? extends V>> valueFunction,
          Supplier<M> multimapSupplier) {
    checkNotNull(keyFunction);
    checkNotNull(valueFunction);
    checkNotNull(multimapSupplier);
    return Collector.of(
        multimapSupplier,
        (multimap, input) -> {
          K key = keyFunction.apply(input);
          Collection<V> valuesForKey = multimap.get(key);
          valueFunction.apply(input).forEachOrdered(valuesForKey::add);
        },
        (multimap1, multimap2) -> {
          multimap1.putAll(multimap2);
          return multimap1;
        });
  }

  public static <T> T checkNotNull(T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }

  public abstract static class MultimapBuilder<
      K0 extends @Nullable Object, V0 extends @Nullable Object> {

    public static MultimapBuilderWithKeys<@Nullable Object> linkedHashKeys() {
      throw new RuntimeException();
    }

    public abstract <K extends K0, V extends V0> Multimap<K, V> build();

    public interface ListMultimap<K extends @Nullable Object, V extends @Nullable Object>
        extends Multimap<K, V> {}

    public interface Multimap<K extends @Nullable Object, V extends @Nullable Object> {

      Collection<V> get(K key);

      void putAll(Multimap<? extends K, ? extends V> multimap2);
    }

    public abstract static class ListMultimapBuilder<
            K0 extends @Nullable Object, V0 extends @Nullable Object>
        extends MultimapBuilder<K0, V0> {}

    public abstract static class MultimapBuilderWithKeys<K0 extends @Nullable Object> {

      public ListMultimapBuilder<K0, @Nullable Object> arrayListValues() {
        throw new RuntimeException();
      }
    }
  }

  public static class ImmutableListMultimap<K, V> extends ImmutableMultimap<K, V>
      implements CollectorCollect.MultimapBuilder.ListMultimap<K, V> {

    public static <K, V> ImmutableListMultimap<K, V> copyOf(
        CollectorCollect.MultimapBuilder.Multimap<? extends K, ? extends V> multimap) {
      throw new RuntimeException();
    }

    @Override
    public Collection<V> get(K key) {
      return null;
    }

    @Override
    public void putAll(MultimapBuilder.Multimap<? extends K, ? extends V> multimap2) {}
  }

  public abstract static class ImmutableMultimap<K, V> {}
}
