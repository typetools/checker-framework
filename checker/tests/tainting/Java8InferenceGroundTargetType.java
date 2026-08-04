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
// AbstractType.getFunctionType() computes.  Using T rather than T' can therefore equate a lambda
// parameter type with an inference variable that T' has already instantiated differently.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Java8InferenceGroundTargetType {

  interface BiSink<A, B> {
    void accept(A a, B b);

    // The target type of the `s` argument is `BiSink<? extends B, U>`.  It mentions the inference
    // variable for `U`, so it is not a proper type and the lambda constraint really is reduced.
    // It is wildcard-parameterized, so its ground target type is inferred per JLS 18.5.3.  `B` is
    // BiSink's own type parameter, so the fresh variable that 18.5.3 creates for `B` does get an
    // instantiation: the wildcard `? extends B` grounds to `B`, which is the type of the function
    // type's first parameter, so that fresh variable is equated with the lambda's first declared
    // parameter type.  That instantiation becomes the second type argument of the ground target
    // type, replacing `U`.
    //
    // Concretely, for the call in `mismatchedSecondParameter` below:
    //   T  = BiSink<? extends B, U>, whose function type has parameter types (B, U)
    //   T' = BiSink<B, @Untainted B>, whose function type has parameter types (B, @Untainted B)
    default <U> U foo(BiSink<? extends B, U> s, U u) {
      return u;
    }

    // The second lambda parameter is @Tainted, but the ground target type's second function type
    // parameter is @Untainted B (taken from the first lambda parameter).  So <F2 = P2> is
    // <@Tainted B = @Untainted B>, a constraint between two proper types, which is dropped.
    // Inference of `U` therefore succeeds, driven by `<T' <: T>` and by the argument `b`, and the
    // only remaining error is the genuine mismatch that the lambda's parameter types are not the
    // parameter types of the function type.
    //
    // Before the fix, `T` was used instead of `T'`, so the constraint was <@Tainted B = U>, where
    // `U` is still an inference variable.  That added `U = @Tainted B` to the bound set, which
    // contradicts `U = @Untainted B` from `<T' <: T>`, and inference failed with
    // (type.arguments.not.inferred).
    default void mismatchedSecondParameter(@Untainted B b) {
      // :: error: [lambda.param]
      @Untainted B r = foo((@Untainted B x, @Tainted B y) -> {}, b);
    }

    // Control: both declared lambda parameter types are the same, so <F2 = P2> and <F2 = U> agree
    // and inference succeeds either way.  This shows that `mismatchedSecondParameter` is sensitive
    // to the second parameter's qualifier and not merely to the shape of the call.
    default void matchingSecondParameter(@Untainted B b) {
      // :: error: [lambda.param]
      @Untainted B r = foo((@Untainted B x, @Untainted B y) -> {}, b);
    }

    // Control: an unannotated explicitly typed lambda, which type-checks cleanly.
    default void unannotated(B b) {
      B r = foo((B x, B y) -> {}, b);
    }

    // Control: an implicitly typed lambda does not go through the <Fi = Pi> constraints at all.
    default void implicitlyTyped(@Untainted B b) {
      @Untainted B r = foo((x, y) -> {}, b);
    }
  }
}
