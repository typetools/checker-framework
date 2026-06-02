package open.falseneg;

import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue6890 {
  static class SomeClass<T> {
    T f;

    SomeClass(T f) {
      this.f = f;
    }

    T getF() {
      return this.f;
    }
  }

  public static void method(Map<@Nullable String, String> map) {
    SomeClass<Map<@Nullable String, String>> x = new SomeClass<Map<@Nullable String, String>>(map);
    // :: error: [assignment]
    Map<String, ?> y = x.getF(); // invalid assignment
  }
}
