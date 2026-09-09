// Two upper bounds on an inference variable whose parameterized supertypes have different
// qualifiers make the variable uninferrable, even though the Java types agree.

import java.util.List;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.Covariant;

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

  // The conflicting bounds both come from the declaration.
  <S extends A<@Untainted String> & B<@Tainted String>> S intersectionBound() {
    throw new RuntimeException();
  }

  void useIntersectionBound() {
    // :: error: [type.arguments.not.inferred]
    Object o = intersectionBound();
  }

  // The qualifiers differ in the second type argument.
  interface Sup2<T, U> {}

  interface A2<T, U> extends Sup2<T, U> {}

  interface B2<T, U> extends Sup2<T, U> {}

  <S extends A2<@Untainted String, @Untainted String>> S twoTypeArgs() {
    throw new RuntimeException();
  }

  void useTwoTypeArgs() {
    // :: error: [type.arguments.not.inferred]
    B2<@Untainted String, @Tainted String> x = twoTypeArgs();
  }

  // The qualifiers differ within a type argument rather than on it.
  <S extends A<List<@Untainted String>>> S nested() {
    throw new RuntimeException();
  }

  void useNested() {
    // :: error: [type.arguments.not.inferred]
    B<List<@Tainted String>> x = nested();
  }

  // The two bounds reach Sup at different depths.
  interface Mid<T> extends Sup<T> {}

  interface Deep<T> extends Mid<T> {}

  <S extends Deep<@Untainted String>> S deep() {
    throw new RuntimeException();
  }

  void useDeep() {
    // :: error: [type.arguments.not.inferred]
    B<@Tainted String> x = deep();
  }

  // The conflict is between two inference variables rather than a variable and a proper type.
  <U extends B<@Tainted String>> void take(U u) {}

  void useNestedCall() {
    // :: error: [type.arguments.not.inferred]
    take(m());
  }

  // Incorporation does not imply a constraint for a wildcard type argument.
  void useWildcard() {
    B<?> x = m();
  }

  // At a covariant type argument the qualifiers need not match, because a type whose supertype is
  // one parameterization can still be a subtype of the other.
  @Covariant(0)
  interface CovariantSup<T> {}

  <S extends CovariantSup<@Untainted String>> S covariant() {
    throw new RuntimeException();
  }

  void useCovariant() {
    CovariantSup<@Tainted String> x = covariant();
  }

  // The same relationship without inference, for comparison.
  CovariantSup<@Untainted String> covariantNoInference() {
    throw new RuntimeException();
  }

  void useCovariantNoInference() {
    CovariantSup<@Tainted String> x = covariantNoInference();
  }
}
