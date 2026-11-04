import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// The least upper bound of a type with an ignored substitution and a type variable.  B = U comes
// from a use with an explicit annotation, so it ignores annotations, and the type argument of
// holderOf's result is an ignored substitution.  The type variable H cannot be viewed as class
// Holder, so the positions of the two types are not aligned.
public class LubOfIgnoredSubstitutionAndTypeVariable {
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
    // The other input is a type variable.  The least upper bound must not keep the origin of
    // holderOf's result, because the ignored substitution in it is not compared with the type
    // argument of H's bound.  If it kept the origin, then substituting instantiations into the
    // origin would give the type argument the instantiation of B, which is @NonNull U.
    // :: error: (argument) :: error: (dereference.of.nullable)
    pick2(holderOf(nonNullU, nn), nullableH).get().toString();
    // The same, with the arguments in the other order.
    // :: error: (argument) :: error: (dereference.of.nullable)
    pick2(nullableH, holderOf(nonNullU, nn)).get().toString();
  }
}
