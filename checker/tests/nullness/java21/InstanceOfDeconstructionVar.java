// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that a binding variable of a deconstruction pattern that is declared using var has the type
// arguments of its record component.

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class InstanceOfDeconstructionVar {

  record Strings(List<@Nullable String> l) {}

  record NonNullStrings(List<String> l) {}

  record Box<T>(T value) {}

  void typeArguments(Object o) {
    if (o instanceof Strings(var l)) {
      // :: error: [dereference.of.nullable]
      l.get(0).length();
    }
  }

  void nonNullTypeArguments(Object o) {
    if (o instanceof NonNullStrings(var l)) {
      l.get(0).length();
    }
  }

  void wildcardComponent(Box<? extends List<@Nullable String>> b) {
    if (b instanceof Box(var l)) {
      // :: error: [dereference.of.nullable]
      l.get(0).length();
    }
  }

  void commentBeforeName(Object o) {
    if (o instanceof Strings(var /* comment */ l)) {
      // :: error: [dereference.of.nullable]
      l.get(0).length();
    }
  }

  void unicodeEscape(Object o) {
    if (o instanceof Strings(var l)) {
      // :: error: [dereference.of.nullable]
      l.get(0).length();
    }
  }
}
