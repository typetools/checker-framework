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

    // :: error: [assignment]
    Map<String, ?> y = x.getF();

    y.keySet().iterator().next().toString();
  }

  static void noFalsePositives() {
    // Assigning non-null key map to non-null key map: no error.
    Map<String, String> m1 = new HashMap<>();
    Holder<Map<String, String>> h1 = new Holder<>(m1);
    Map<String, ?> ok1 = h1.getF();

    // Assigning nullable-key map to wildcard-key map: no error (wildcard accepts anything).
    Map<@Nullable String, String> m2 = new HashMap<>();
    Holder<Map<@Nullable String, String>> h2 = new Holder<>(m2);
    Map<@Nullable String, ?> ok2 = h2.getF();

    // Assigning non-null value to non-null variable: no error.
    Holder<String> h3 = new Holder<>("hello");
    String ok3 = h3.getF();
  }
}
