import java.util.Set;

@SuppressWarnings("all") // Just check for crashes
public class MyMapCrash<K, V> {
  public static class ImmutableSetMultimap<S, T> {
    public ImmutableSetMultimap(MyMapCrash<S, Set<T>> map) {}
  }

  private final class MapSet extends MyMapCrash<K, Set<V>> {}

  public void asMultimap() {
    new ImmutableSetMultimap<K, V>(new MapSet());
  }
}
