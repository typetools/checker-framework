import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

class MyKey {}

class Issue7082a {

  void method(MyKey key) {
    Map<MyKey, Integer> a = new HashMap<>();
    a.merge(
        key,
        -1,
        (Integer old, Integer v) -> {
          if (old + v == 0) {
            return null;
          } else {
            return old + v;
          }
        });
  }
}

class MapMerge7082 {

  void method(MyKey key) {
    Map<MyKey, Integer> a = new HashMap<>();
    a.merge(key, -1, MapMerge7082::mergeFunction);
  }

  private static @Nullable Integer mergeFunction(Integer old, Integer v) {
    if (old + v == 0) {
      return null;
    } else {
      return old + v;
    }
  }
}
