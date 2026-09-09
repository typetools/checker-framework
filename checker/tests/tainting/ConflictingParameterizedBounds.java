// Two upper bounds on an inference variable whose parameterized supertypes have different
// qualifiers make the variable uninferrable, even though the Java types agree.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class ConflictingParameterizedBounds {

  interface Sup<T> {}

  interface A<T> extends Sup<T> {}

  interface B<T> extends Sup<T> {}

  <S extends A<@Untainted String>> S m() {
    throw new RuntimeException();
  }

  void useDifferentQualifiers() {
    // S must be a subtype of both A<@Untainted String> and B<@Tainted String>, and so of both
    // Sup<@Untainted String> and Sup<@Tainted String>.  No such type exists.
    // :: error: [type.arguments.not.inferred]
    B<@Tainted String> x = m();
  }

  void useSameQualifiers() {
    B<@Untainted String> x = m();
  }
}
