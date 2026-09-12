import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.Covariant;

// An equality constraint between two proper types compares their qualifiers.
public class ProperTypeEquality {
  interface Sup<T> {}

  interface A<T> extends Sup<T> {}

  interface B<T> extends Sup<T> {}

  <S extends A<@Untainted String>> S m() {
    throw new RuntimeException();
  }

  void useDifferentQualifiers() {
    // S has upper bounds A<@Untainted String> and B<@Tainted String>, so S would have to be a
    // subtype of both Sup<@Untainted String> and Sup<@Tainted String>.  The implied constraint
    // `@Untainted String = @Tainted String` does not hold.
    // :: error: [type.arguments.not.inferred]
    B<@Tainted String> x = m();
  }

  void useSameQualifiers() {
    B<@Untainted String> x = m();
  }

  @Covariant(0)
  interface CovariantSup<T> {}

  <S extends CovariantSup<@Untainted String>> S covariantM() {
    throw new RuntimeException();
  }

  void useCovariantTypeArgument() {
    // The type argument of `CovariantSup` is covariant, so S can be a subtype of both
    // CovariantSup<@Untainted String> and CovariantSup<@Tainted String>; the implied constraint
    // between the two type arguments does not compare their qualifiers.
    CovariantSup<@Tainted String> x = covariantM();
  }
}
