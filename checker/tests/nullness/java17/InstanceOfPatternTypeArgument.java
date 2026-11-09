// @below-java17-jdk-skip-test

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class InstanceOfPatternTypeArgument {

  static class Box<T extends @NonNull Object> {}

  static class Sub<T extends @NonNull Object> extends Box<T> {}

  void instanceOfPattern(Box<String> b) {
    // :: error: (type.argument)
    if (b instanceof Sub<@Nullable String> s) {}
  }

  void localVariable() {
    // :: error: (type.argument)
    Sub<@Nullable String> s = null;
  }
}
