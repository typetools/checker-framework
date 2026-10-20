import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java11-jdk-skip-test
public class Issue4170 {
  public <K, V> void loadSequentially(Iterable<? extends K> keys) {
    Map<K, @Nullable V> result = new LinkedHashMap<>();
    for (K key : keys) {
      result.put(key, null);
    }
    for (var iter = result.entrySet().iterator(); iter.hasNext(); ) {
      var entry = iter.next();
    }
  }

  public void method1() {
    var s = new ArrayList<@Nullable String>();
    for (var str : s) {}
  }

  public void method2() {
    var s = new ArrayList<@Nullable ArrayList<@Nullable String>>();
  }
}
