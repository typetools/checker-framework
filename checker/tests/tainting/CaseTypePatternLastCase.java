// @below-java21-jdk-skip-test

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

// The last case of an exhaustive switch is not tested at run time, but its binding variable is
// still refined by the qualifiers of the selector expression.
public class CaseTypePatternLastCase {

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  void rule(@Untainted Shape s) {
    switch (s) {
      case Circle c -> {
        @Untainted Object x = c;
      }
      case Square q -> {
        @Untainted Object x = q;
      }
    }
  }

  @Untainted Object expression(@Untainted Shape s) {
    return switch (s) {
      case Circle c -> c;
      case Square q -> q;
    };
  }

  void statementGroup(@Untainted Shape s) {
    switch (s) {
      case Circle c:
        @Untainted Object x = c;
        break;
      case Square q:
        @Untainted Object y = q;
    }
  }

  <T extends @Untainted Shape> void typeVariable(T s) {
    switch (s) {
      case Circle c -> {
        @Untainted Object x = c;
      }
      case Square q -> {
        @Untainted Object x = q;
      }
    }
  }

  void tainted(@Tainted Shape s) {
    switch (s) {
      case Circle c -> {}
      case Square q -> {
        // :: error: (assignment)
        @Untainted Object x = q;
      }
    }
  }
}
