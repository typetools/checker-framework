// A bound from a use of an inference variable that ignores annotations relates only Java types.
// Reduced from Beam JavaBeanUtils.java:423 (Comparator.nullsFirst(Comparator.naturalOrder())).
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class IgnoredUseOfVariable {
  interface Cmp<T extends @Nullable Object> {}

  static <T extends @NonNull Object> Cmp<T> nonNullCmp() {
    throw new RuntimeException();
  }

  static <T extends @Nullable Object> Cmp<@Nullable T> nullsFirst(Cmp<? super T> c) {
    throw new RuntimeException();
  }

  static <T extends @Nullable Object, U extends @Nullable Object> Cmp<T> comparing(
      Function<? super T, ? extends U> f, Cmp<? super U> c) {
    throw new RuntimeException();
  }

  // U :> @Nullable K from `f`, and U <: @Nullable T' from nullsFirst's return type.  The second
  // relates only the Java types of U and T', so it must not make T' of nonNullCmp() nullable.
  static <T extends @Nullable Object, K extends @Nullable Object> Cmp<T> m(
      Function<? super T, ? extends @Nullable K> f) {
    return comparing(f, nullsFirst(nonNullCmp()));
  }
}
