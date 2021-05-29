import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class ObjectArrayParam {
  void test(@UnknownInitialization Object... args) {
    for (Object obj : args) {
      boolean isClass = obj instanceof Class<?>;
      // :: error: initialization.cast
      @Initialized @NonNull Class<?> clazz = (isClass ? (@Initialized @NonNull Class<?>) obj : obj.getClass());
    }
  }
}
