import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

// @below-java21-jdk-skip-test

// The type of a binding variable declared by a type pattern in a case label is checked against the
// type of the selector expression, as one declared by an instanceof pattern is.
public class CaseTypePatternUnsafe {

  void untainted(@Untainted Object o) {
    switch (o) {
      case @Untainted String s -> {}
      default -> {}
    }
  }

  void tainted(@Tainted Object o) {
    switch (o) {
      // :: warning: (case.pattern.unsafe)
      case @Untainted String s -> {}
      case @Tainted Integer i -> {}
      case Long l -> {}
      default -> {}
    }
  }

  int switchExpression(@Tainted Object o) {
    return switch (o) {
      // :: warning: (case.pattern.unsafe)
      case @Untainted String s -> 0;
      default -> 1;
    };
  }
}
