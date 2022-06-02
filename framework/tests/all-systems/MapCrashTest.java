import java.util.Set;

@SuppressWarnings("all") // Just check for crashes
public class MapCrashTest<K, V> {
    public static class ImmutableSetMultimap<S, T> {
        public ImmutableSetMultimap(MapCrashTest<S, Set<T>> map) {}
    }

    private final class MapSet extends MapCrashTest<K, Set<V>> {}

    public void asMultimap() {
        new ImmutableSetMultimap<K, V>(new MapSet());
    }
}
