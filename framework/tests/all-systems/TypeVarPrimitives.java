// Annotated versions in
// checker/tests/nullness/TypeVarPrimitivesNullness.java and
// checker/tests/interning/TypeVarPrimitivesInterning.java
public class TypeVarPrimitives {
  <T extends Long> void method(T tLong) {
    long l = tLong;
  }

  <T extends Long & Cloneable> void methodIntersection(T tLong) {
    long l = tLong;
  }

  // Regression tests for https://github.com/typetools/checker-framework/issues/8328.
  <T extends Integer> int add(T t) {
    return t + 1;
  }

  <T extends Long> long subtract(T t) {
    return 1 - t;
  }

  <T extends Integer> int assign(T t) {
    int result = t;
    return result;
  }

  <T extends Integer> boolean equality(T t) {
    return t == 1;
  }

  <T extends Integer> int conditional(boolean b, T t) {
    return b ? t : 0;
  }

  <T extends Integer> long bitwise(T t) {
    return t & 1L;
  }

  <T extends Integer> boolean lessThan(T t) {
    return t < 1;
  }

  <T extends Boolean> boolean booleanOps(T b) {
    return (b & true) || b == false;
  }

  long captured(java.util.List<? extends Integer> l) {
    return l.get(0) * 2L;
  }
}
