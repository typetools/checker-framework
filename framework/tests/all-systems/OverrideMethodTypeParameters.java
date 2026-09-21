// Overriding a generic method, which requires comparing the two methods' type parameters.

import java.util.Comparator;
import java.util.List;

@SuppressWarnings("all") // Just check for crashes.
public class OverrideMethodTypeParameters {

  static class Super<C> {
    <T> T identity(T p) {
      return p;
    }

    <T extends Comparable<T>, U extends T> T max(T p1, U p2) {
      return p1;
    }

    <T extends C> List<T> wrap(T p) {
      throw new Error("not implemented");
    }

    <T> void unused() {}
  }

  static class Sub<C> extends Super<C> {
    // Rename the type parameters.
    @Override
    <S> S identity(S p) {
      return p;
    }

    @Override
    <A extends Comparable<A>, B extends A> A max(A p1, B p2) {
      return p1;
    }

    @Override
    <T extends C> List<T> wrap(T p) {
      throw new Error("not implemented");
    }

    @Override
    <T> void unused() {}
  }

  // A method reference is checked as an override of the functional interface's method.  A lambda
  // is not; BaseTypeVisitor.visitLambdaExpression checks the lambda's parameters and body instead.
  static <T> Comparator<T> comparators(Comparator<T> c) {
    Comparator<T> c1 = (p1, p2) -> c.compare(p1, p2);
    Comparator<T> c2 = c::compare;
    return c1;
  }

  static <T extends Comparable<T>> T useMax(Super<Object> s, T p1, T p2) {
    return s.<T, T>max(p1, p2);
  }
}
