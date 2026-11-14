// The Interning Checker runs on this file with -Acheckclass=InterningCheckClass.A, so it checks a
// comparison only if an operand's value might be an instance of A.
public class InterningCheckClass {

  static class A {}

  static class SubA extends A {}

  interface I {}

  static final class FinalUnrelated {}

  boolean sameClass(A x, A y) {
    // :: error: [not.interned]
    return x == y;
  }

  boolean subclass(SubA x, SubA y) {
    // :: error: [not.interned]
    return x == y;
  }

  boolean superclass(Object x, Object y) {
    // :: error: [not.interned]
    return x == y;
  }

  // A subclass of `A` can implement `I`.
  boolean interfaceType(I x, I y) {
    // :: error: [not.interned]
    return x == y;
  }

  // A caller can instantiate `T` to `A`.
  <T> boolean typeVariable(T x, T y) {
    // :: error: [not.interned]
    return x == y;
  }

  <T extends A> boolean typeVariableBoundedByClass(T x, T y) {
    // :: error: [not.interned]
    return x == y;
  }

  // No `FinalUnrelated` is an `A`.
  boolean unrelatedFinalClass(FinalUnrelated x, FinalUnrelated y) {
    return x == y;
  }

  // No `String` is an `A`.
  boolean string(String x, String y) {
    return x == y;
  }
}
