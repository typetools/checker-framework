import checkers.nullness.quals.*;

public class Uninit6 {
  // Failure to initialize these fields does not directly compromise the
  // guarantee of no null pointer errors.
  @LazyNonNull Object f;
  @Nullable Object g;
  // int i;  // TODO: Discuss the implication of primitive field initialization
  //:: warning: (fields.uninitialized)
  Uninit6() { }
}
