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

  // A reference comparison: neither operand is unboxed.  The Interning Checker correctly reports
  // these comparisons.
  @SuppressWarnings("interning")
  <T extends Integer> boolean referenceEquality(T t1, T t2) {
    return t1 == t2;
  }

  @SuppressWarnings("interning")
  <T extends Boolean> boolean referenceEqualityBoolean(T t1, T t2) {
    return t1 != t2;
  }

  <T extends Integer> int conditional(boolean b, T t) {
    return b ? t : 0;
  }

  // The type of a conditional expression with a null operand is a reference type, here Integer.
  <T extends Integer> void conditionalWithNull(boolean b, T t) {
    Integer result = b ? t : null;
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

  <T extends Boolean> boolean conditionalOps(T b) {
    return (b && true) || (b || false);
  }

  <T extends Boolean> boolean compoundBoolean(T t) {
    boolean x = true;
    x &= t;
    x |= t;
    x ^= t;
    return x;
  }

  <T extends Integer> int unaryMinus(T t) {
    return -t;
  }

  <T extends Integer> int compoundAddition(T t) {
    int x = 1;
    x += t;
    return x;
  }

  <T extends Integer> int shift(T t) {
    return t >> 1;
  }

  <T extends Boolean> int ifCondition(T b) {
    if (b) {
      return 1;
    }
    return 0;
  }

  long captured(java.util.List<? extends Integer> l) {
    return l.get(0) * 2L;
  }
}
