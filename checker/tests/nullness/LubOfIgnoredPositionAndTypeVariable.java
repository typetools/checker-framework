import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// The least upper bound of a type with an ignored position and a type variable.  B = U comes from a
// use with an explicit annotation, so it ignores annotations, and the type argument of holderOf's
// result is an ignored position.  The positions of the type variable H are those of its upper
// bound, Holder<@Nullable U>.
public class LubOfIgnoredPositionAndTypeVariable {
  static class Box<T> {}

  static class Holder<E extends @Nullable Object> {
    E get() {
      throw new RuntimeException();
    }
  }

  static <B extends @Nullable Object> Holder<B> holderOf(Box<@NonNull B> b, B other) {
    throw new RuntimeException();
  }

  static <S extends @Nullable Object> S pick2(S a, S b) {
    return a;
  }

  static <U extends @Nullable Object, H extends Holder<@Nullable U>> void test(
      Box<@NonNull U> nonNullU, @NonNull U nn, H nullableH, Holder<@Nullable U> nullableHolder) {
    // The other input is not a type variable.
    // :: error: (argument) :: error: (dereference.of.nullable)
    pick2(holderOf(nonNullU, nn), nullableHolder).get().toString();
    // The other input is a type variable whose upper bound is Holder<@Nullable U>.
    // :: error: (argument) :: error: (dereference.of.nullable)
    pick2(holderOf(nonNullU, nn), nullableH).get().toString();
    // :: error: (argument) :: error: (dereference.of.nullable)
    pick2(nullableH, holderOf(nonNullU, nn)).get().toString();
  }
}
