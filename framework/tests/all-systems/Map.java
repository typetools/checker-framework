import java.util.Set;

@SuppressWarnings("all") // Just check for crashes
public class Map<K, V> {
  public static class ImmutableSetMultimap<S, T> {
    public ImmutableSetMultimap(Map<S, Set<T>> map) {}
  }

  private final class MapSet extends Map<K, Set<V>> {}

  public void asMultimap() {
    new ImmutableSetMultimap<K, V>(new MapSet());
  }
}
