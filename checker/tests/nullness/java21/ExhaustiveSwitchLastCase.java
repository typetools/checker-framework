// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that if no case of an exhaustive switch matches, control does not exit the switch normally.

import org.checkerframework.checker.nullness.qual.Nullable;

public class ExhaustiveSwitchLastCase {

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  String switchExpression(Shape shape) {
    String r =
        switch (shape) {
          case Circle c -> "a";
          case Square s -> "b";
        };
    return r;
  }

  int switchStatement(Shape shape) {
    @Nullable String x = null;
    switch (shape) {
      case Circle c -> x = "a";
      case Square s -> x = "b";
    }
    return x.length();
  }

  int switchStatementColon(Shape shape) {
    @Nullable String x = null;
    switch (shape) {
      case Circle c:
        x = "a";
        break;
      case Square s:
        x = "b";
    }
    return x.length();
  }
}
