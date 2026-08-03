// Tests that an explicitly typed lambda whose target type is wildcard-parameterized is checked
// against the function type of the ground target type T' (JLS 18.2.1), where T' is computed by
// functional interface parameterization inference (JLS 18.5.3), rather than against the function
// type of the target type T itself.
//
// This is a regression test for the interaction between that rule and the reduction of an equality
// constraint between two proper types (JLS 18.2.4).  Before both were implemented, the constraint
// <F1 = G1> below was <@IntVal(1) Integer = T>, whose two sides have different Java types.  That
// constraint was accepted only because equality between proper types unconditionally reduced to
// true.  Once that rule is implemented, the constraint reduces to false, and a false bound during
// argument-constraint reduction is a crash ("BugInCF: False bound for ..."), not a type error.
// With the ground target type computed correctly, G1 is @IntVal(1) Integer and the constraint
// reduces to true.
//
// The Value Checker is used so that the lambda parameter can carry a qualifier that differs from
// the one on the type parameter bound.

import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.UnknownVal;

public class ExplicitLambdaGroundTargetType {

  interface Fn<T extends @UnknownVal Integer> {
    T apply(T t);
  }

  // The target type of the lambda is Fn<? extends T>, which is wildcard-parameterized and, because
  // it mentions the inference variable for T, is not a proper type.  Both properties are required
  // to reach JLS 18.5.3: a proper target type short-circuits in JLS 18.2.1.
  <T extends @UnknownVal Integer> void upperBounded(Fn<? extends T> f) {}

  <T extends @UnknownVal Integer> void lowerBounded(Fn<? super T> f) {}

  void explicitlyTypedLambdaUpperBoundedWildcard() {
    upperBounded(
        (@IntVal(1) Integer p) -> p);
  }

  void explicitlyTypedLambdaLowerBoundedWildcard() {
    lowerBounded(
        (@IntVal(1) Integer p) -> p);
  }

  // An implicitly typed lambda takes the other branch of JLS 15.27.3: the ground target type is the
  // non-wildcard parameterization of T, and no <Fi = Gi> constraints are created.
  void implicitlyTypedLambdaWildcard() {
    upperBounded(p -> p);
  }
}
