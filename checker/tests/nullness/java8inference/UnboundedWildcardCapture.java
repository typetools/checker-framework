// JLS 18.3.2, incorporation of a capture bound `G<α1, ..., αn> = capture(G<A1, ..., An>)`: if Ai is
// an unbounded wildcard `?`, then `αi <: R` implies the constraint formula `‹Bi θ <: R›`, where Bi
// is the bound of G's i-th type parameter.
//
// `VariableBounds.getWildcardConstraints` used to state that rule in a comment but not implement
// it, so the unbounded-wildcard case produced no constraint at all.
//
// Below, the return type `Pair<?, U>` is wildcard-parameterized, so 18.5.2.1 introduces the capture
// bound `Pair<β1, β2> = capture(Pair<?, α>)`.  The target type gives β1 the upper bound
// `@NonNull Object`, and B1 θ is `@Nullable Object`, so `‹@Nullable Object <: @NonNull Object›` is
// implied and inference fails.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class UnboundedWildcardCapture {

  interface Pair<A extends @Nullable Object, B extends @Nullable Object> {}

  static <U> Pair<?, U> make(U u) {
    throw new RuntimeException();
  }

  void use() {
    // :: error: [type.arguments.not.inferred] :: error: [assignment]
    Pair<? extends @NonNull Object, String> p = make("x");
  }

  // The same call whose target type permits the type parameter's own bound is consistent.
  void ok() {
    Pair<? extends @Nullable Object, String> p = make("x");
  }
}
