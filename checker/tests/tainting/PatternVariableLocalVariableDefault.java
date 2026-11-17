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
    // :: error: (assignment)
    local = tainted;
    // :: warning: (instanceof.pattern.unsafe)
    if (o instanceof String s) {
      // :: error: (assignment)
      s = tainted;
    }
    switch (o) {
      // :: warning: (case.pattern.unsafe)
      case String s -> {
        // :: error: (assignment)
        s = tainted;
      }
      default -> {}
    }
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
