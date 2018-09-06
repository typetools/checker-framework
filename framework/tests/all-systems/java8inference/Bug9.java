package inference.guava;

import java.util.Collection;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.Function;

@SuppressWarnings("") // Just check for crashes.
public class Bug9<K, Z> {
    private transient Map<K, Collection<Z>> map;

    Spliterator<Z> valueSpliterator() {
        return flatMap(
                map.values().spliterator(), Collection::spliterator, Spliterator.SIZED, size());
    }

    static <F, T> Spliterator<T> flatMap(
            Spliterator<F> fromSpliterator,
            Function<? super F, Spliterator<T>> function,
            int topCharacteristics,
            long topSize) {
        throw new RuntimeException();
    }

    public int size() {
        throw new RuntimeException();
    }
}
