import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

// The upper bound of a use of an F-bounded type variable, such as `@Tainted S` where S is declared
// as `S extends Store<S>`, is `@Tainted Store<@Tainted S>`.  The qualifier on that type argument is
// a copy of the qualifier on the use rather than an invariant type argument that a programmer
// wrote, so type inference must not compare it to the qualifier of another type argument.
public class FBoundedTypeArgument<S extends FBoundedTypeArgument.Store<S>> {

  interface Store<T extends Store<T>> {}

  static class Box<V, T> {}

  static <V, T extends Store<T>> T run(Box<V, T> box) {
    throw new RuntimeException();
  }

  void useDifferentQualifiers(Box<Object, @Untainted S> box) {
    // T is @Untainted S, whose upper bound is @Untainted Store<@Untainted S>, whereas the upper
    // bound of the target type @Tainted S is @Tainted Store<@Tainted S>.
    @Tainted S x = run(box);
  }

  void useSameQualifiers(Box<Object, @Untainted S> box) {
    @Untainted S x = run(box);
  }
}
