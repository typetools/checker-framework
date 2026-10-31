// @below-java21-jdk-skip-test

// Test that the MatchException thrown when no case of an exhaustive switch matches does not cause a
// false positive.

import java.io.FileInputStream;
import java.io.IOException;

public class ExhaustiveSwitchMatchException {

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  int switchExpression(Shape shape) throws IOException {
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
    FileInputStream s = new FileInputStream("x");
    int r = 0;
    switch (shape) {
      case Circle c -> r = 1;
      case Square q -> r = 2;
    }
    s.close();
    return r;
  }
}
