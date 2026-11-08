// @below-java21-jdk-skip-test

// Test that a resource leaks along the MatchException path when the guard of the last case of an
// exhaustive switch is false.

import java.io.FileInputStream;
import java.io.IOException;

public class GuardedLastCaseMatchException {

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  int switchExpression(Shape shape, boolean b) throws IOException {
    // :: error: [required.method.not.called]
    FileInputStream s = new FileInputStream("x");
    int r =
        switch (shape) {
          case Circle c -> 1;
          case Square q -> 2;
          case Shape x when b -> 3;
        };
    s.close();
    return r;
  }

  int switchStatement(Shape shape, boolean b) throws IOException {
    // :: error: [required.method.not.called]
    FileInputStream s = new FileInputStream("x");
    int r = 0;
    switch (shape) {
      case Circle c -> r = 1;
      case Square q -> r = 2;
      case Shape x when b -> r = 3;
    }
    s.close();
    return r;
  }
}
