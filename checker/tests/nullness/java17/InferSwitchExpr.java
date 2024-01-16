// @below-java17-jdk-skip-test

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class InferSwitchExpr {
  enum Letter {
    A,
    B,
    C,
    D;
  }

  <T> List<T> singletonList(T t) {
    throw new RuntimeException();
  }

  @Nullable List<String> method(Letter letter, boolean a, boolean b) {
    return switch (letter) {
      case A -> {
        if (a) {
          if (b) {
            // :: error: (type.arguments.not.inferred)
            yield singletonList(null);
          }
          // :: error: (type.arguments.not.inferred)
          yield singletonList(null);
        }
        // :: error: (type.arguments.not.inferred)
        yield singletonList(null);
      }
      case B -> null;
      case C -> null;
      case D -> null;
    };
  }
}
