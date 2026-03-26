// @below-java17-jdk-skip-test

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ArrowSwitchInference {
  enum Letter {
    A,
    B,
    C,
    D;
  }

  <T> List<T> singletonList(T t) {
    throw new RuntimeException();
  }

  <D> D id(D p) {
    return p;
  }

  @Nullable List<String> method(Letter letter, boolean a, boolean b) {
    return id(
        switch (letter) {
          case A -> singletonList((String) null);
          case B -> singletonList("");
          case C -> null;
          case D -> null;
        });
  }

  @Nullable List<@Nullable String> method2(Letter letter, boolean a, boolean b) {
    return id(
        switch (letter) {
          case A -> singletonList((String) null);
          case B -> singletonList("");
          case C -> null;
          case D -> null;
        });
  }
}
