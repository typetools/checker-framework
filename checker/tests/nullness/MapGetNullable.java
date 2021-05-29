import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MapGetNullable {

  void foo0(Map<String, @Nullable Integer> m, @KeyFor("#1") String key) {
    // :: error: (assignment)
    @NonNull Integer val = m.get(key);
  }

  <K, V> V get0(Map<K, V> m, @KeyFor("#1") String key) {
    return m.get(key);
  }

  public static class MyMap1<K, V> extends HashMap<K, V> {
    // TODO: These test cases do not work yet, because of the generic types.
    // void useget(@KeyFor("this") String k) {
    //     V val = get(k);
    // }
    // void useget2(@KeyFor("this") String k) {
    //     V val = this.get(k);
    // }
  }

  void foo1(MyMap1<String, @Nullable Integer> m, @KeyFor("#1") String key) {
    // :: error: (assignment)
    @NonNull Integer val = m.get(key);
  }

  <K, V> V get1(MyMap1<K, V> m, @KeyFor("#1") String key) {
    return m.get(key);
  }

  public static class MyMap2<V, K> extends HashMap<K, V> {}

  void foo2(MyMap2<@Nullable Integer, String> m, @KeyFor("#1") String key) {
    // :: error: (assignment)
    @NonNull Integer val = m.get(key);
  }

  <K, V> V get2(MyMap2<V, K> m, @KeyFor("#1") String key) {
    return m.get(key);
  }

  public static class MyMap3<K> extends HashMap<K, @Nullable Integer> {}

  void foo3(MyMap3<String> m, @KeyFor("#1") String key) {
    // :: error: (assignment)
    @NonNull Integer val = m.get(key);
  }

  <K> @Nullable Integer get3(MyMap3<K> m, @KeyFor("#1") String key) {
    return m.get(key);
  }

  public static class MyMap4<K> extends HashMap<K, Integer> {}

  void foo4(MyMap4<String> m, @KeyFor("#1") String key) {
    Integer val = m.get(key);
  }

  <K> Integer get4(MyMap4<K> m, @KeyFor("#1") String key) {
    return m.get(key);
  }

  public static class MyMap5<V> extends HashMap<String, V> {}

  void foo5(MyMap5<@Nullable Integer> m, @KeyFor("#1") String key) {
    // :: error: (assignment)
    @NonNull Integer val = m.get(key);
  }

  <V> V get5(MyMap5<V> m, @KeyFor("#1") String key) {
    return m.get(key);
  }

  public static class MyMap6 extends HashMap<String, Integer> {
    void useget(@KeyFor("this") String k) {
      @NonNull Integer val = get(k);
    }

    void useget2(@KeyFor("this") String k) {
      @NonNull Integer val = this.get(k);
    }
  }

  void foo6(MyMap6 m, @KeyFor("#1") String key) {
    @NonNull Integer val = m.get(key);
  }

  Integer get6(MyMap6 m, @KeyFor("#1") String key) {
    return m.get(key);
  }

  public static class MyMap7 extends HashMap<String, @Nullable Integer> {
    void useget(@KeyFor("this") String k) {
      // :: error: (assignment)
      @NonNull Integer val = get(k);
    }

    void useget2(@KeyFor("this") String k) {
      // :: error: (assignment)
      @NonNull Integer val = this.get(k);
    }
  }

  void foo7(MyMap7 m, @KeyFor("#1") String key) {
    // :: error: (assignment)
    @NonNull Integer val = m.get(key);
  }

  Integer get7(MyMap7 m, @KeyFor("#1") String key) {
    // :: error: (return)
    return m.get(key);
  }

  // MyMap9 ensures that no changes are made to the return type of overloaded versions of get().

  public static class MyMap9<K, V> extends HashMap<K, V> {
    @Nullable V get(@Nullable Object key, int itIsOverloaded) {
      return null;
    }
  }

  void foo9(MyMap9<String, @Nullable Integer> m, @KeyFor("#1") String key) {
    // :: error: (assignment)
    @NonNull Integer val = m.get(key);
  }

  void foo9a(MyMap9<String, @Nullable Integer> m, @KeyFor("#1") String key) {
    // :: error: (assignment)
    @NonNull Integer val = m.get(key, 22);
  }

  <K, V> V get9(MyMap9<K, V> m, @KeyFor("#1") String key) {
    return m.get(key);
  }

  <K, V> V get9a(MyMap9<K, V> m, @KeyFor("#1") String key) {
    // :: error: (return)
    return m.get(key, 22);
  }
}
