// JLS 18.3.2, incorporation of a capture bound `G<α1, ..., αn> = capture(G<A1, ..., An>)`: if Ai is
// an unbounded wildcard `?`, then `αi <: R` implies the constraint formula `‹Bi θ <: R›`, where Bi
// is the bound of G's i-th type parameter.

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
