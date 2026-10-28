// Test case for https://github.com/typetools/checker-framework/issues/1440

import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue1440<T> {

  public static <T> Issue1440<T> get(T object) {
    return new Issue1440<>(object);
  }

  private final @Nullable T value;

  private Issue1440(@Nullable T value) {
    this.value = value;
  }

  public <U> @Nullable U map(Function<T, @Nullable U> mappingFunction) {
    if (value == null) {
      return null;
    }
    return mappingFunction.apply(value);
  }

  static void use() {
    Object obj = 2.14;
    Object o = Issue1440.get(obj).map(Object::getClass);
  }
}
