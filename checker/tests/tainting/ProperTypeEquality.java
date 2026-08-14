import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

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
}
