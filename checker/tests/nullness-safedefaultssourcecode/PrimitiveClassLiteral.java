import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class PrimitiveClassLiteral {
  private static @Nullable Class<?> unwrapPrimitive(Class<?> c) {
    if (c == Byte.class) {
      return byte.class;
    }
    if (c == Character.class) {
      return char.class;
    }
    if (c == Short.class) {
      return short.class;
    }
    if (c == Integer.class) {
      return int.class;
    }
    if (c == Long.class) {
      return long.class;
    }
    if (c == Float.class) {
      return float.class;
    }
    if (c == Double.class) {
      return double.class;
    }
    if (c == Boolean.class) {
      return boolean.class;
    }
    return c;
  }
}
