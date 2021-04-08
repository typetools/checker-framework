// Test case for Issue 596:
// https://github.com/typetools/checker-framework/issues/596

import java.util.concurrent.atomic.AtomicReference;
import org.checkerframework.checker.nullness.qual.*;

public class Issue596 {

  private static String getOrEmpty(AtomicReference<String> ref) {
    return Optional596.fromNullable(ref.get()).or("");
  }
}

// From Google Guava
class Optional596<T> {

  public static <T> Optional596<T> fromNullable(@Nullable T nullableReference) {
    return new Optional596<T>();
  }

  public T or(T defaultValue) {
    return defaultValue;
  }
}
