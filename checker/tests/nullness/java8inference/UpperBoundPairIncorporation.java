// The last paragraph of JLS 18.3.1: when a bound set contains a pair of bounds `α <: S` and
// `α <: T`, and there is a supertype of S of the form `G<S1, ..., Sn>` and a supertype of T of the
// form `G<T1, ..., Tn>`, then `‹Si = Ti›` is implied.

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class UpperBoundPairIncorporation {

  interface Common<E extends @Nullable Object> {}

  interface G<E extends @Nullable Object> extends Common<E> {}

  interface H<E extends @Nullable Object> extends Common<E> {}

  static <E extends @Nullable Object, T extends G<E>> T make(E e) {
    throw new RuntimeException();
  }

  void use(@Nullable String nullable) {
    // :: error: [type.arguments.not.inferred]
    H<@NonNull String> h = make(nullable);
  }

  // The same call with a @Nullable type argument in the target type is consistent.
  void ok(@Nullable String nullable) {
    H<@Nullable String> h = make(nullable);
  }
}
