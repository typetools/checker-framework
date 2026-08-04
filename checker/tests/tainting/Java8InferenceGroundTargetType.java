// Test that reduction of a constraint formula <LambdaExpression -> T> for an explicitly typed
// lambda expression uses the ground target type T', not the target type T.
//
// JLS 18.2.1 says that if the lambda expression is explicitly typed, and T' is the ground target
// type, then "let P1, ..., Pn be the parameter types of the function type of T', and let F1, ...,
// Fn be the parameter types of the lambda expression.  The constraint reduces to ... <F1 = P1>,
// ..., <Fn = Pn>".
//
// When T is a wildcard-parameterized functional interface type, T' is inferred as described in
// JLS 18.5.3 and may differ from the non-wildcard parameterization of T that
// AbstractType.getFunctionType() computes.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Java8InferenceGroundTargetType {

  interface BiSink<A, B> {
    void accept(A a, B b);

    // The target type of the `s` argument is `BiSink<? extends B, U>`.  It mentions the inference
    // variable for `U`, so it is not a proper type and the lambda constraint really is reduced.
    // It is wildcard-parameterized, so its ground target type is inferred per JLS 18.5.3: fresh
    // inference variables alpha1 and alpha2 are created for BiSink's type parameters `A` and `B`;
    // the function type of `BiSink<alpha1, alpha2>` has parameter types `(alpha1, alpha2)`; and
    // reducing <Pi = alphai>, where Pi is the i'th declared parameter type of the lambda,
    // instantiates alpha1 and alpha2 to the lambda's declared parameter types.  Each instantiation
    // becomes the corresponding type argument of the ground target type, replacing `? extends B`
    // and `U`.
    //
    // Concretely, for the call in `mismatchedSecondParameter` below:
    //   T  = BiSink<? extends B, U>, whose function type has parameter types (B, U)
    //   T' = BiSink<@Untainted B, @Tainted B>, whose function type has parameter types
    //        (@Untainted B, @Tainted B)
    default <U> U foo(BiSink<? extends B, U> s, U u) {
      return u;
    }

    // The parameter types of the ground target type's function type are the lambda's declared
    // parameter types, so <F1 = P1> and <F2 = P2> are trivially true.  JLS 18.5.3 also requires
    // T' <: T; because T mentions the inference variable `U`, that requirement is reduced as a
    // constraint, and it yields U = @Tainted B.  The call therefore returns @Tainted B, which is
    // not assignable to the @Untainted B variable and which contradicts the bound U <: @Untainted B
    // that the assignment context imposes, so inference has no solution.
    default void mismatchedSecondParameter(@Untainted B b) {
      // :: error: [assignment] :: error: [type.arguments.not.inferred]
      @Untainted B r = foo((@Untainted B x, @Tainted B y) -> {}, b);
    }

    // Control: both declared lambda parameter types are the same, so T' is
    // BiSink<@Untainted B, @Untainted B> and T' <: T yields U = @Untainted B, which agrees with the
    // argument `b` and with the assignment context.  Inference therefore succeeds, and the only
    // remaining error is the genuine mismatch that the lambda's parameter types are not the
    // parameter types of the function type.  This shows that `mismatchedSecondParameter` is
    // sensitive to the second parameter's qualifier and not merely to the shape of the call.
    default void matchingSecondParameter(@Untainted B b) {
      // :: error: [lambda.param]
      @Untainted B r = foo((@Untainted B x, @Untainted B y) -> {}, b);
    }

    // Control: an unannotated explicitly typed lambda, which type-checks cleanly.
    default void unannotated(B b) {
      B r = foo((B x, B y) -> {}, b);
    }

    // Control: an implicitly typed lambda does not go through JLS 18.5.3 or the <Fi = Pi>
    // constraints at all.  Its ground target type is the non-wildcard parameterization of T.
    default void implicitlyTyped(@Untainted B b) {
      @Untainted B r = foo((x, y) -> {}, b);
    }
  }
}
