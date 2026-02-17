import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

class Holder<T> {
  T f;

  Holder(T f) {
    this.f = f;
  }

  T getF() {
    return f;
  }
}

public class WildcardNullableKey {
  public static void main(String[] args) {
    Map<@Nullable String, String> map = new HashMap<>();
    map.put(null, "");

    Holder<Map<@Nullable String, String>> x = new Holder<>(map);

    // :: error: (assignment)
    Map<String, ?> y = x.getF();

    y.keySet().iterator().next().toString();
  }
}
