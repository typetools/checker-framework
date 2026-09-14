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
    // Sup<@Untainted String> and Sup<@Tainted String>.  The implied equality constraint
    // `@Untainted String = @Tainted String` does not hold, so no such type exists.
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
    // S can be a subtype of both CovariantSup<@Untainted String> and CovariantSup<@Tainted
    // String>, so the implied constraint between the two type arguments ignores the qualifiers.
    CovariantSup<@Tainted String> x = covariant();
  }

  // The same relationship without inference, for comparison.
  CovariantSup<@Untainted String> covariantNoInference() {
    throw new RuntimeException();
  }

  void useCovariantNoInference() {
    CovariantSup<@Tainted String> x = covariantNoInference();
  }

  // Only the covariant argument is exempt.
  @Covariant(1)
  interface CovariantSecond<T, U> {}

  <S extends CovariantSecond<@Untainted String, @Untainted String>> S covariantSecond() {
    throw new RuntimeException();
  }

  void useCovariantSecondArg() {
    CovariantSecond<@Untainted String, @Tainted String> x = covariantSecond();
  }

  void useCovariantFirstArg() {
    // :: error: [assignment] :: error: [type.arguments.not.inferred]
    CovariantSecond<@Tainted String, @Untainted String> x = covariantSecond();
  }

  // A covariant type argument nested inside an invariant one must still match.  The two bounds
  // relate S to each parameterization, but relate the two parameterizations to each other by
  // equality, which `@Covariant` does not relax.
  <X, S extends A<CovariantSup<Pair<X, @Untainted String>>>> S covariantNestedArg(X x) {
    throw new RuntimeException();
  }

  void useCovariantNestedArg(String s) {
    // :: error: [type.arguments.not.inferred]
    B<CovariantSup<Pair<String, @Tainted String>>> x = covariantNestedArg(s);
  }

  // The conflicting type argument mentions an inference variable, so it is not a proper type
  // until the constraint is reduced.
  interface Pair<X, Y> {}

  <X, S extends A<Pair<X, @Untainted String>>> S typeArgWithVariable(X x) {
    throw new RuntimeException();
  }

  void useTypeArgWithVariable(String s) {
    // :: error: [type.arguments.not.inferred]
    B<Pair<String, @Tainted String>> x = typeArgWithVariable(s);
  }

  // The qualifier that differs is on a type argument that mentions an inference variable, so the
  // constraint between the two type arguments is not between two proper types and reduces to a
  // constraint on their type arguments.
  interface Holder<X> {}

  <X, S extends A<@Untainted Holder<X>>> S qualifierOnTypeArgWithVariable(X x) {
    throw new RuntimeException();
  }

  void useQualifierOnTypeArgWithVariable(String s) {
    // :: error: [type.arguments.not.inferred]
    B<@Tainted Holder<String>> x = qualifierOnTypeArgWithVariable(s);
  }

  // Likewise for the qualifier on an array whose component type mentions an inference variable.
  <X, S extends A<X @Untainted []>> S qualifierOnArrayWithVariable(X x) {
    throw new RuntimeException();
  }

  void useQualifierOnArrayWithVariable(String s) {
    // :: error: [type.arguments.not.inferred]
    B<String @Tainted []> x = qualifierOnArrayWithVariable(s);
  }

  // The qualifiers differ in the bound of a wildcard type argument.  The bound mentions an
  // inference variable that is not yet instantiated, so the constraint between the two wildcards
  // is not between two proper types and reduces to a constraint between their bounds.
  <X, S extends A<Pair<Object, ? extends @Untainted Holder<X>>>> S wildcardUpperBound() {
    throw new RuntimeException();
  }

  void useWildcardUpperBound() {
    // :: error: [type.arguments.not.inferred]
    B<Pair<Object, ? extends @Tainted Holder<String>>> x = wildcardUpperBound();
  }

  <X, S extends A<Pair<Object, ? super @Untainted Holder<X>>>> S wildcardLowerBound() {
    throw new RuntimeException();
  }

  void useWildcardLowerBound() {
    // :: error: [type.arguments.not.inferred]
    B<Pair<Object, ? super @Tainted Holder<String>>> x = wildcardLowerBound();
  }

  // The inference variable that the wildcard's bound mentions is instantiated before the
  // constraint between the two wildcards is reduced, so both wildcards are proper types.  Their
  // qualifiers are on their bounds, which an uncaptured wildcard does not expose to the type
  // hierarchy, so the constraint must still reduce to a constraint between the bounds.
  <X, S extends A<Pair<X, ? extends @Untainted Holder<X>>>> S properWildcardUpperBound(X x) {
    throw new RuntimeException();
  }

  void useProperWildcardUpperBound(String s) {
    // :: error: [type.arguments.not.inferred]
    B<Pair<String, ? extends @Tainted Holder<String>>> x = properWildcardUpperBound(s);
  }

  void useProperWildcardUpperBoundSameQualifiers(@Untainted String s) {
    B<Pair<@Untainted String, ? extends @Untainted Holder<@Untainted String>>> x =
        properWildcardUpperBound(s);
  }

  <X, S extends A<Pair<X, ? super @Untainted Holder<X>>>> S properWildcardLowerBound(X x) {
    throw new RuntimeException();
  }

  void useProperWildcardLowerBound(String s) {
    // :: error: [type.arguments.not.inferred]
    B<Pair<String, ? super @Tainted Holder<String>>> x = properWildcardLowerBound(s);
  }

  // TODO: This is a false negative.  No such S exists, but the conflict is in the argument of an
  // enclosing type, and neither the constraints implied by incorporation nor the equality
  // constraint between two declared types cover enclosing type arguments.
  static class Outer<O> {
    class In {}
  }

  <S extends A<Outer<@Untainted String>.In>> S enclosingTypeArg() {
    throw new RuntimeException();
  }

  void useEnclosingTypeArg() {
    B<Outer<@Tainted String>.In> x = enclosingTypeArg();
  }

  // When the enclosing type mentions an inference variable, a constraint between the two enclosing
  // types is created and the qualifiers of their type arguments are compared.
  <X, S extends A<Outer<@Untainted Holder<X>>.In>> S enclosingTypeArgWithVariable(X x) {
    throw new RuntimeException();
  }

  void useEnclosingTypeArgWithVariable(String s) {
    // :: error: [type.arguments.not.inferred]
    B<Outer<@Tainted Holder<String>>.In> x = enclosingTypeArgWithVariable(s);
  }
}
