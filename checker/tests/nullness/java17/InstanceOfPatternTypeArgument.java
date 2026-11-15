// @below-java17-jdk-skip-test

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class InstanceOfPatternTypeArgument {

  static class Box<T extends @NonNull Object> {}

  static class Sub<T extends @NonNull Object> extends Box<T> {}

  void instanceofPattern(Box<String> b) {
    // :: error: (type.argument)
    if (b instanceof Sub<@Nullable String> s) {}
  }

  void instanceofNoPattern(Box<String> b) {
    // :: error: (type.argument)
    if (b instanceof Sub<@Nullable String>) {}
  }

  void instanceofValid(Box<String> b) {
    if (b instanceof Sub<String> s) {}
    if (b instanceof Sub<String>) {}
  }

  void localVariable() {
    // :: error: (type.argument)
    Sub<@Nullable String> s = null;
  }
}
