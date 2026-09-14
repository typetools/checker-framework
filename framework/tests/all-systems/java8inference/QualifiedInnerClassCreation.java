// Test case for a qualified class instance creation expression (`outer.new Inner<>(...)`) that is
// itself a poly expression.  For such an expression,
// `InferenceFactory.getTypeOfMethodAdaptedToUse` walks up the enclosing types of the receiver
// looking for the class that declares the constructor.  The walk never finds that class, because
// the receiver's type is the *enclosing* class rather than the constructed class.  Without a
// `break` after the `NewClassTree` fallback, the walk fell through to a `(DeclaredType) enclosing`
// cast applied to the `NONE`-kinded enclosing type of a top-level class, and threw a
// ClassCastException.

public class QualifiedInnerClassCreation {

  class Inner<T> {
    Inner(T t) {}
  }

  class Mid {
    class Innermost<T> {
      Innermost(T t) {}
    }
  }

  static <U> void consume(U u) {}

  // The receiver's type is the top-level class, whose enclosing type has kind NONE, so the walk
  // reaches the fallback on its first iteration.
  static void diamond(QualifiedInnerClassCreation outer) {
    consume(outer.new Inner<>("hello"));
  }

  // The receiver's type is itself an inner class, so the walk takes more than one iteration
  // before reaching the NONE-kinded enclosing type.
  static void nested(QualifiedInnerClassCreation.Mid mid) {
    consume(mid.new Innermost<>("hello"));
  }
}
