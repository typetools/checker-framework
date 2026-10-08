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
}
