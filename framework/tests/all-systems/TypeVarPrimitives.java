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

  <T extends Integer> int enhancedForArray(T[] ts) {
    int sum = 0;
    for (int x : ts) {
      sum += x;
    }
    return sum;
  }

  <T extends Integer> int enhancedForIterable(java.util.List<T> ts) {
    int sum = 0;
    for (int x : ts) {
      sum += x;
    }
    return sum;
  }

  long enhancedForWidening(java.util.List<Integer> integers) {
    long sum = 0;
    for (long x : integers) {
      sum += x;
    }
    return sum;
  }

  <T extends Integer> long enhancedForWideningTypeVar(java.util.List<T> ts, T[] tArray) {
    long sum = 0;
    for (long x : ts) {
      sum += x;
    }
    for (long x : tArray) {
      sum += x;
    }
    return sum;
  }

  long captured(java.util.List<? extends Integer> l) {
    return l.get(0) * 2L;
  }

  long enhancedForWildcard(java.util.List<? extends Integer> l) {
    long sum = 0;
    for (int x : l) {
      sum += x;
    }
    for (long x : l) {
      sum += x;
    }
    return sum;
  }

  <T extends Integer> long enhancedForWildcardTypeVar(java.util.List<? extends T> l) {
    long sum = 0;
    for (int x : l) {
      sum += x;
    }
    for (long x : l) {
      sum += x;
    }
    return sum;
  }

  abstract static class IntegerBox<T extends Integer> implements Iterable<T> {}

  // The capture of the wildcard is bounded by Integer, the declared bound of T.
  long enhancedForUnboundedWildcard(IntegerBox<?> box) {
    long sum = 0;
    for (int x : box) {
      sum += x;
    }
    for (long x : box) {
      sum += x;
    }
    return sum;
  }
}
