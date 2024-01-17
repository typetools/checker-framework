// Test case for Issue 1098:
// https://github.com/typetools/checker-framework/issues/1098

import java.util.Optional;

public class Issue1098 {
  <T> void opt(Optional<T> p1, T p2) {}

  <T> void cls(Class<T> p1, T p2) {}

  @SuppressWarnings("keyfor:type.argument")
  void use() {
    opt(Optional.empty(), null);
    cls(this.getClass(), null);
  }
}
