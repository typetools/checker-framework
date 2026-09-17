// The type arguments of an enclosing type are type arguments of an inner class type, so two bounds
// on an inference variable imply constraints between the enclosing types' type arguments, just as
// they do between the inner class type's own type arguments.
//
// A qualifier written on the type argument of an enclosing type is dropped (issue #8170), so every
// non-default qualifier below reaches an enclosing type argument through the type of a receiver.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.Covariant;

public class EnclosingTypeArgumentBounds {

  static class Outer<T> {
    class Sup<U> {}

    class Sub<U> extends Sup<U> {}

    void useSup(Sup<@Tainted String> s) {}

    void useB(B<Sup<@Tainted String>> b) {}
  }

  interface Sup2<T> {}

  interface A<T> extends Sup2<T> {}

  interface B<T> extends Sup2<T> {}

  <S extends Outer<String>.Sup<String>> S sup() {
    throw new RuntimeException();
  }

  // Both bounds are parameterizations of the same class, and only their enclosing type arguments
  // differ.
  void useSameClass(Outer<@Untainted String> untainted, Outer<@Tainted String> tainted) {
    // S must be a subtype of both Outer<@Tainted String>.Sup<@Tainted String> (its declared
    // bound) and Outer<@Untainted String>.Sup<@Tainted String> (the target type), so the implied
    // constraint `@Tainted String = @Untainted String` between the enclosing type arguments does
    // not hold.
    // :: error: [type.arguments.not.inferred]
    untainted.useSup(sup());

    tainted.useSup(sup());
  }

  <S extends Outer<String>.Sub<String>> S sub() {
    throw new RuntimeException();
  }

  // The common parameterized supertype of the two bounds is an inner class type.
  void useSubclass(Outer<@Untainted String> untainted, Outer<@Tainted String> tainted) {
    // :: error: [type.arguments.not.inferred]
    untainted.useSup(sub());

    tainted.useSup(sub());
  }

  <X, S extends A<Outer<String>.Sup<X>>> S nested() {
    throw new RuntimeException();
  }

  // The enclosing types are compared within an equality constraint between two type arguments,
  // one of which mentions an inference variable.
  void useNested(Outer<@Untainted String> untainted, Outer<@Tainted String> tainted) {
    // The bounds imply `Outer<@Tainted String>.Sup<X> = Outer<@Untainted String>.Sup<@Tainted
    // String>`, which reduces to `X = @Tainted String` and to a constraint between the enclosing
    // types, which does not hold.
    // :: error: [type.arguments.not.inferred]
    untainted.useB(nested());

    tainted.useB(nested());
  }

  @Covariant(0)
  static class CovariantOuter<T> {
    class Inner<U> {}

    void useInner(Inner<@Tainted String> i) {}
  }

  <S extends CovariantOuter<String>.Inner<String>> S covariantOuter() {
    throw new RuntimeException();
  }

  // At a covariant type argument of the enclosing type the qualifiers need not match, just as at a
  // covariant type argument of the inner class type itself.
  void useCovariantOuter(CovariantOuter<@Untainted String> untainted) {
    untainted.useInner(covariantOuter());
  }

  static class RawOuter<T> {
    class Inner<U> {}

    void useRawInner(RawOuter.Inner i) {}
  }

  <S extends RawOuter<String>.Inner<String>> S rawInner() {
    throw new RuntimeException();
  }

  // A raw type has no type arguments, so no constraint relates it to the other bound's.
  void useRaw(RawOuter<@Untainted String> untainted) {
    untainted.useRawInner(rawInner());
  }
}
