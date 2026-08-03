import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AnnotatedTypeVar {

  static <X> X id(X x) {
    return x;
  }

  static <T, U> U annotatedTypeArgIsSecond(Map<T, @Nullable U> m, T t) {
    throw new RuntimeException();
  }

  Object useAnnotatedTypeArgIsSecond(Map<String, @Nullable Integer> m, String s) {
    return annotatedTypeArgIsSecond(id(m), id(s));
  }

  static <T, U> T annotatedTypeArgIsFirst(Map<@Nullable U, T> m, T t) {
    throw new RuntimeException();
  }

  Object useAnnotatedTypeArgIsFirst(Map<@Nullable Integer, String> m, String s) {
    return annotatedTypeArgIsFirst(id(m), id(s));
  }
}
