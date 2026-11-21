// @below-java21-jdk-skip-test

// Test that a resource leaks along the MatchException path when no case of an exhaustive switch
// matches, and that a finally block that closes the resource prevents the leak.

import java.io.FileInputStream;
import java.io.IOException;

public class ExhaustiveSwitchMatchException {

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  int switchExpression(Shape shape) throws IOException {
    // :: error: [required.method.not.called]
    FileInputStream s = new FileInputStream("x");
    int r =
        switch (shape) {
          case Circle c -> 1;
          case Square q -> 2;
        };
    s.close();
    return r;
  }

  int switchStatement(Shape shape) throws IOException {
    // :: error: [required.method.not.called]
    FileInputStream s = new FileInputStream("x");
    int r = 0;
    switch (shape) {
      case Circle c -> r = 1;
      case Square q -> r = 2;
    }
    s.close();
    return r;
  }

  int closedInFinally(Shape shape) throws IOException {
    FileInputStream s = new FileInputStream("x");
    try {
      return switch (shape) {
        case Circle c -> 1;
        case Square q -> 2;
      };
    } finally {
      s.close();
    }
  }
}
