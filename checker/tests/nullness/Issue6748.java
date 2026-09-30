// Test case for https://github.com/typetools/checker-framework/issues/6748

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

class Issue6748 {

  abstract static class Typed<T> {

    abstract T get();
  }

  <T> Optional<T> method1(Typed<? extends @Nullable T> arg) {
    return Optional.ofNullable(arg.get());
  }

  <T> Optional<T> method2(Typed<? extends @Nullable T> arg) {
    return method1(arg);
  }
}
