// Test the inference of the ground target type T' of an explicitly typed lambda expression whose
// target type T is a wildcard-parameterized functional interface type.
//
// JLS 18.2.1 says that if the lambda expression is explicitly typed, and T' is the ground target
// type, then "let P1, ..., Pn be the parameter types of the function type of T', and let F1, ...,
// Fn be the parameter types of the lambda expression.  The constraint reduces to ... <F1 = P1>,
// ..., <Fn = Pn>", together with <T' <: T>.
//
// JLS 18.5.3 infers T' as follows: give each of F's m type parameters a fresh inference variable
// a1, ..., am; equate the lambda's declared parameter types with the parameter types of the
// function type of F<a1, ..., am>; then, for each type argument Ai of T, use ai's instantiation if
// the resulting bound set contains one, and Ai otherwise.  A type argument of T is therefore
// replaced even when it is not a wildcard.
//
// The second type argument of T in this test is the inference variable for `U`, and 18.5.3
// replaces it, so <T' <: T> determines `U` from the lambda's second declared parameter type.
//
// A companion test, Java8InferenceGroundTargetType2, tests that reduction uses the function type
// of T' rather than the function type of T.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Java8InferenceGroundTargetType {

  interface BiSink<A, B> {
    void accept(A a, B b);

    // The target type of the `s` argument is `BiSink<? extends B, U>`.  It mentions the inference
    // variable for `U`, so it is not a proper type and the lambda constraint really is reduced.
    // It is wildcard-parameterized, so its ground target type is inferred per JLS 18.5.3.  Both
    // fresh variables get instantiations, because both of BiSink's type parameters appear as
    // parameter types of BiSink's function type, so both type arguments of T are replaced: the
    // wildcard `? extends B` and the inference variable `U` alike.
    //
    // Concretely, for the call in `mismatchedSecondParameter` below:
    //   T  = BiSink<? extends B, U>
    //   T' = BiSink<@Untainted B, @Tainted B>, whose function type has parameter types
    //        (@Untainted B, @Tainted B), which are the lambda's declared parameter types
    default <U> U foo(BiSink<? extends B, U> s, U u) {
      return u;
    }

    // Because 18.5.3 built T' out of the lambda's declared parameter types, both <F1 = P1> and
    // <F2 = P2> are constraints between identical proper types, which hold.  `U` is then
    // determined by <T' <: T>, which equates `U` with the second type argument of T', namely
    // @Tainted B.  The call therefore returns @Tainted B, which is not a subtype of the
    // @Untainted B type of the local variable, so no instantiation of `U` satisfies all the
    // bounds and inference fails.
    default void mismatchedSecondParameter(@Untainted B b) {
      // :: error: [assignment] :: error: [type.arguments.not.inferred]
      @Untainted B r = foo((@Untainted B x, @Tainted B y) -> {}, b);
    }

    // Control: the second lambda parameter is @Untainted, so <T' <: T> gives `U = @Untainted B`
    // and the assignment succeeds.  This shows that `mismatchedSecondParameter` is sensitive to
    // the second parameter's qualifier and not merely to the shape of the call.  The remaining
    // error is from checking the lambda against the function type of T, whose first parameter
    // type grounds the wildcard `? extends B` to `B`, which is @Tainted.
    default void matchingSecondParameter(@Untainted B b) {
      // :: error: [lambda.param]
      @Untainted B r = foo((@Untainted B x, @Untainted B y) -> {}, b);
    }

    // Control: an unannotated explicitly typed lambda, which type-checks cleanly.
    default void unannotated(B b) {
      B r = foo((B x, B y) -> {}, b);
    }

    // Control: an implicitly typed lambda does not go through 18.5.3 or the <Fi = Pi> constraints
    // at all.
    default void implicitlyTyped(@Untainted B b) {
      @Untainted B r = foo((x, y) -> {}, b);
    }
  }
}
