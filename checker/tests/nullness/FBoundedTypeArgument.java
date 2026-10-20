import org.checkerframework.checker.nullness.qual.Nullable;

// The upper bound of a use of an F-bounded type variable, such as `@Nullable S` where S is declared
// as `S extends Store<S>`, is `@Nullable Store<@Nullable S>`.  The qualifier on that type argument
// is a copy of the qualifier on the use rather than an invariant type argument that a programmer
// wrote, so type inference must not compare it to the qualifier of another type argument.  This is
// the shape of `AnalysisResult.runAnalysisFor` in the dataflow framework.
public class FBoundedTypeArgument<V, S extends FBoundedTypeArgument.Store<S>> {

  interface Store<Q extends Store<Q>> {}

  static class TransferInput<A, B> {}

  static <A, B extends Store<B>> B run(TransferInput<A, B> input) {
    throw new RuntimeException();
  }

  // The upper bound of the target type @Nullable S is @Nullable Store<@Nullable S>, whereas the
  // upper bound of S, the type argument that the parameter implies, is @NonNull Store<S>.
  @Nullable S useDifferentQualifiers(TransferInput<V, S> input) {
    return run(input);
  }

  S useSameQualifiers(TransferInput<V, S> input) {
    return run(input);
  }
}
