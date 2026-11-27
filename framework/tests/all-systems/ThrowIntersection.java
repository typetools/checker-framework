// Tests a throw statement whose expression has an intersection type.

public class ThrowIntersection {
  interface I {}

  static class A extends RuntimeException implements I {}

  static class B extends RuntimeException implements I {}

  void conditional(boolean b) {
    throw b ? new A() : new B();
  }

  void cast(Object x) {
    throw (RuntimeException & I) x;
  }
}
