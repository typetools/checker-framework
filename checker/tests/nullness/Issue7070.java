package open.falsepos;

import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue7070 {
  static class MyClass<Z> {}

  <T> @Nullable T getNullable(MyClass<T> type) {
    throw new RuntimeException();
  }

  static <T> Optional<@NonNull T> ofNullable(@Nullable T value) {
    return Optional.ofNullable(value);
  }

  <T> Optional<@NonNull T> getOpt(int index, MyClass<T> type) {
    return ofNullable(getNullable(type));
  }
}
