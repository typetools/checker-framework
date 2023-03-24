import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java10-jdk-skip-test
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
    @NonNull String s = "s";
    var v = s;
  }

  public void method2() {
    var s = new ArrayList<@NonNull String>();
  }
}
