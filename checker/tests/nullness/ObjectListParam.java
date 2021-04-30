import java.util.List;
import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class ObjectListParam {
  // :: error: type.argument
  void test(List<@UnknownInitialization Object> args) {
    for (Object obj : args) {
      boolean isClass = obj instanceof Class<?>;
      // :: error: initialization.cast
      @Initialized Class<?> clazz = (isClass ? (@Initialized Class<?>) obj : obj.getClass());
    }
  }
}
