// Test case for https://github.com/typetools/checker-framework/issues/3624

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

public class Issue3624<T> {
  private final T t0;

  Issue3624(T t0) {
    this.t0 = t0;
  }

  public @PolyNull Object[] toArray(Issue3624<@PolyNull T> this) {
    // :: error: (new.array)
    Object[] res = new Object[1];
    res[0] = t0;
    return res;
  }

  public @PolyNull Object[] toArray2(Issue3624<@PolyNull T> this) {
    return new Object[] {t0};
  }

  static void useNonNull(Issue3624<String> l) {
    l.toArray()[0].toString();
    l.toArray2()[0].toString();
  }

  static void useNullable(Issue3624<@Nullable String> l) {
    // :: error: (dereference.of.nullable)
    l.toArray()[0].toString();
    // :: error: (dereference.of.nullable)
    l.toArray2()[0].toString();
  }
}
