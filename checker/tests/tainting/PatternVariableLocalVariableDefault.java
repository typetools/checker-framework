// @below-java21-jdk-skip-test

import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

// A default for TypeUseLocation.LOCAL_VARIABLE applies to a pattern variable, as it does to a local
// variable.
public class PatternVariableLocalVariableDefault {

  @DefaultQualifier(value = Untainted.class, locations = TypeUseLocation.LOCAL_VARIABLE)
  void withDefault(Object o, String tainted) {
    String local = "";
    // :: error: [assignment]
    local = tainted;
    // :: warning: [instanceof.pattern.unsafe]
    if (o instanceof String s) {
      // :: error: [assignment]
      s = tainted;
    }
    switch (o) {
      case String s -> {
        // :: error: [assignment]
        s = tainted;
      }
      default -> {}
    }
  }

  record Box(String s) {}

  @DefaultQualifier(value = Untainted.class, locations = TypeUseLocation.LOCAL_VARIABLE)
  void varBindingWithDefault(Box b) {
    // The `var` binding variable gets the @Untainted default, but the component is tainted.
    // :: error: [assignment]
    if (b instanceof Box(var s)) {}
  }

  void varBindingWithoutDefault(Box b) {
    if (b instanceof Box(var s)) {}
  }

  void withoutDefault(Object o, String tainted) {
    String local = "";
    local = tainted;
    if (o instanceof String s) {
      s = tainted;
    }
    switch (o) {
      case String s -> {
        s = tainted;
      }
      default -> {}
    }
  }
}
