// A polymorphic qualifier that reaches inference from an enclosing invocation is not a
// QualifierVar, so requiring it to equal a concrete qualifier would reject every instantiation.

import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class PolymorphicQualifierBound {

  interface Sup<T> {}

  interface A<T> extends Sup<T> {}

  interface B<T> extends Sup<T> {}

  interface Holder<X> {}

  void takePolyString(A<@PolyTainted String> a) {}

  <S extends B<@Tainted String>> S taintedBound() {
    throw new RuntimeException();
  }

  <S extends B<@Untainted String>> S untaintedBound() {
    throw new RuntimeException();
  }

  // Without inference, @PolyTainted is instantiated to the qualifier of the argument.
  void noInference(A<@Tainted String> tainted, A<@Untainted String> untainted) {
    takePolyString(tainted);
    takePolyString(untainted);
  }

  // The target type of the invocation is one upper bound of S and its declared bound is another,
  // so incorporation implies `@PolyTainted String = @Tainted String` between the type arguments of
  // their common supertype Sup.  Both sides of that constraint are proper types.
  void properTypes() {
    takePolyString(taintedBound());
    takePolyString(untaintedBound());
  }

  // The type argument mentions an inference variable, so the implied constraint is not between two
  // proper types and reduces to a QualifierTyping constraint instead.
  void takePolyHolder(A<@PolyTainted Holder<String>> a) {}

  <X, S extends B<@Tainted Holder<X>>> S taintedHolder(X x) {
    throw new RuntimeException();
  }

  <X, S extends B<@Untainted Holder<X>>> S untaintedHolder(X x) {
    throw new RuntimeException();
  }

  void nonProperTypes(String s) {
    takePolyHolder(taintedHolder(s));
    takePolyHolder(untaintedHolder(s));
  }

  // A conflict between two concrete qualifiers is still reported.
  void takeTainted(A<@Tainted String> a) {}

  void concreteQualifiers() {
    takeTainted(taintedBound());
    // :: error: [type.arguments.not.inferred]
    takeTainted(untaintedBound());
  }

  // A polymorphic qualifier nested in a type argument makes the implied constraint compare two
  // whole parameterized types, so it must not suppress the comparison of the other type argument.
  interface Pair<T, U> {}

  void takePolyPair(A<Pair<@PolyTainted String, @Tainted String>> a) {}

  <S extends B<Pair<@Tainted String, @Tainted String>>> S matchingPair() {
    throw new RuntimeException();
  }

  <S extends B<Pair<@Tainted String, @Untainted String>>> S mismatchedPair() {
    throw new RuntimeException();
  }

  void nestedPolymorphicQualifier() {
    takePolyPair(matchingPair());
    // :: error: [type.arguments.not.inferred]
    takePolyPair(mismatchedPair());
  }
}
