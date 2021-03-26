// Test case for Issue 1098:
// https://github.com/typetools/checker-framework/issues/1098

import java.util.Optional;

public class Issue1098 {
  <T> void opt(Optional<T> p1, T p2) {}

  <T> void cls(Class<T> p1, T p2) {}

  @SuppressWarnings("keyfor:type.argument.type.incompatible")
  void use() {
    opt(Optional.empty(), null);
    // TODO: false positive, because type argument inference does not account for @Covariant.
    // See https://github.com/typetools/checker-framework/issues/979.
    // :: error: (argument.type.incompatible)
    cls(this.getClass(), null);
  }
}
