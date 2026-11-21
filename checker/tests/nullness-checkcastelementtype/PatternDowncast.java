// @below-java21-jdk-skip-test

// Test that a downcast, in a cast or in a pattern, does not crash when -AcheckCastElementType is
// passed.

import org.checkerframework.checker.nullness.qual.Nullable;

public class PatternDowncast {

  record Box(Object value) {}

  record NBox(@Nullable Object value) {}

  void cast(Object o) {
    Integer i = (Integer) o;
    i.intValue();
  }

  void typePattern(Object o) {
    if (o instanceof Integer i) {
      i.intValue();
    }
  }

  void nullableTypePattern(@Nullable Object o) {
    if (o instanceof Integer i) {
      i.intValue();
    }
  }

  void deconstructionPattern(Object o) {
    if (o instanceof Box(Integer i)) {
      i.intValue();
    }
    if (o instanceof NBox(Integer i)) {
      i.intValue();
    }
  }
}
