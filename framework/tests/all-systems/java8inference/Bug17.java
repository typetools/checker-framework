import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

public abstract class Bug17<KK, VV> implements Map<KK, VV> {
  public MapBug17<KK, VV> asMultimap(MapBug17<KK, VV> multimapView) {
    if (isEmpty()) {
      return MapBug17.of();
    }
    MapBug17<KK, VV> result = multimapView;
    return (result == null)
        ? (multimapView = new MapBug17<>(new SubSubBug17(), size(), null))
        : result;
  }

  public static class MapBug17<K1, V1> {
    MapBug17(Bug17<K1, MyClass<V1>> map, int size, Comparator<? super V1> valueComparator) {}

    public static <K, V> MapBug17<K, V> of() {
      throw new RuntimeException();
    }
  }

  abstract static class SubBug17<K, V> extends Bug17<K, V> {}

  static class MyClass<Q> {}

  private class SubSubBug17 extends SubBug17<KK, MyClass<VV>> {

    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public boolean containsKey(Object key) {
      return false;
    }

    @Override
    public boolean containsValue(Object value) {
      return false;
    }

    @Override
    public MyClass<VV> get(Object key) {
      return null;
    }

    @Override
    public MyClass<VV> put(KK key, MyClass<VV> value) {
      return null;
    }

    @Override
    public MyClass<VV> remove(Object key) {
      return null;
    }

    @Override
    public void putAll(Map<? extends KK, ? extends MyClass<VV>> m) {}

    @Override
    public void clear() {}

    @Override
    public Set<KK> keySet() {
      return null;
    }

    @Override
    public Collection<MyClass<VV>> values() {
      return null;
    }

    @Override
    public Set<Entry<KK, MyClass<VV>>> entrySet() {
      return null;
    }
  }
}
