// @below-java21-jdk-skip-test

import org.checkerframework.checker.nullness.qual.Nullable;

// A binding variable declared by a type pattern in a case label is non-null, even if the selector
// expression is nullable.
public class CaseTypePatternNullableSelector {

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  int rule(@Nullable Object o) {
    switch (o) {
      case String s -> {
        return s.length();
      }
      case null, default -> {
        return 0;
      }
    }
  }

  String lastCase(@Nullable Shape s) {
    return switch (s) {
      case Circle c -> c.toString();
      case Square q -> q.toString();
      case null -> "";
    };
  }
}
