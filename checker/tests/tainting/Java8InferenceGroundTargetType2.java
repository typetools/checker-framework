// Test that reduction of a constraint formula <LambdaExpression -> T> for an explicitly typed
// lambda expression uses the ground target type T', not the target type T.
//
// JLS 18.2.1 says that if the lambda expression is explicitly typed, and T' is the ground target
// type, then "let P1, ..., Pn be the parameter types of the function type of T', and let F1, ...,
// Fn be the parameter types of the lambda expression.  The constraint reduces to ... <F1 = P1>,
// ..., <Fn = Pn>".
//
// When T is a wildcard-parameterized functional interface type, T' is inferred as described in
// JLS 18.5.3, which replaces each wildcard by the corresponding lambda parameter type.  The
// function type of T, by contrast, grounds each wildcard to its bound, and that bound may mention
// an inference variable.  So using T rather than T' can equate a lambda parameter type with an
// inference variable, which over-constrains inference.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Java8InferenceGroundTargetType2 {

  interface Sink<A> {
    void accept(A a);
  }

  // The target type of the `s` argument is `Sink<? extends U>`.  It mentions the inference variable
  // for `U`, so it is not a proper type and the lambda constraint really is reduced.  It is
  // wildcard-parameterized and the lambdas below are explicitly typed, so its ground target type is
  // inferred per JLS 18.5.3: the wildcard is replaced by the lambda's declared parameter type.
  //
  // Concretely, for the call in `mismatchedParameter` below:
  //   T  = Sink<? extends U>, whose function type has parameter types (U)
  //   T' = Sink<@Untainted String>, whose function type has parameter types (@Untainted String)
  static <U> U foo(Sink<? extends U> s, U u) {
    return u;
  }

  // <F1 = P1> is <@Untainted String = @Untainted String>, a constraint between two proper types,
  // which is dropped.  Inference of `U` therefore succeeds, driven by `<T' <: T>`, which gives the
  // lower bound `@Untainted String <: U`, and by the argument `tainted`, which gives the lower
  // bound `@Tainted String <: U`.  So `U` is `@Tainted String`, and the only remaining error is the
  // genuine mismatch that the lambda's declared parameter type is not the parameter type of the
  // function type of `Sink<? extends @Tainted String>`.
  //
  // Were `T` used instead of `T'`, the constraint would be <@Untainted String = U>, where `U` is
  // still an inference variable.  That would add `U = @Untainted String` to the bound set, which
  // contradicts `@Tainted String <: U` from the argument, and inference would fail with
  // (type.arguments.not.inferred).
  void mismatchedParameter(@Tainted String tainted) {
    // :: error: [lambda.param]
    @Tainted String r = foo((@Untainted String x) -> {}, tainted);
  }

  // Control: the lambda's declared parameter type is the type of the `u` argument, so <F1 = P1> and
  // <F1 = U> agree and inference succeeds either way.  This shows that `mismatchedParameter` is
  // sensitive to the lambda parameter's qualifier and not merely to the shape of the call.
  void matchingParameter(@Untainted String untainted) {
    @Untainted String r = foo((@Untainted String x) -> {}, untainted);
  }

  // Control: an unannotated explicitly typed lambda, which type-checks cleanly.
  void unannotated(String s) {
    String r = foo((String x) -> {}, s);
  }

  // Control: an implicitly typed lambda does not go through the <Fi = Pi> constraints at all.
  void implicitlyTyped(@Tainted String tainted) {
    @Tainted String r = foo((x) -> {}, tainted);
  }
}
