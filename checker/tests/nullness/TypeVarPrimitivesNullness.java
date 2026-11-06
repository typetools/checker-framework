// Unannotated version in framework/tests/all-systems/TypeVarPrimitives.java

import org.checkerframework.checker.nullness.qual.*;

public class TypeVarPrimitivesNullness {
  <T extends @Nullable Long> void method(T tLong) {
    // :: error: [unboxing.of.nullable]
    long l = tLong;
  }

  <T extends @Nullable Long & @Nullable Cloneable> void methodIntersection(T tLong) {
    // :: error: [unboxing.of.nullable]
    long l = tLong;
  }

  <T extends @Nullable Long> void method2(@NonNull T tLong) {
    long l = tLong;
  }

  <T extends @Nullable Long & @Nullable Cloneable> void methodIntersection2(@NonNull T tLong) {
    long l = tLong;
  }

  <T extends @Nullable Long> void method3(@Nullable T tLong) {
    // :: error: [unboxing.of.nullable]
    long l = tLong;
  }

  <T extends @Nullable Long & @Nullable Cloneable> void methodIntersection3(@Nullable T tLong) {
    // :: error: [unboxing.of.nullable]
    long l = tLong;
  }

  <T extends @Nullable Integer> void add(T t) {
    // :: error: [unboxing.of.nullable]
    int result = t + 1;
    // Make sure CFG construction was sufficient to capture that t must be non-null at this point,
    // since the unboxing must have succeeded
    t.toString();
  }

  <T extends @Nullable Integer> void assign(T t) {
    // :: error: [unboxing.of.nullable]
    int result = t;
    t.toString();
  }

  void takeLong(long l) {}

  <T extends @Nullable Integer> void invoke(T t) {
    // :: error: [unboxing.of.nullable]
    takeLong(t);
    t.toString();
  }

  <T extends @Nullable Integer> void equality(T t) {
    // :: error: [unboxing.of.nullable]
    boolean result = t == 1;
    t.toString();
  }

  <T extends @Nullable Integer> void bitwise(T t) {
    // :: error: [unboxing.of.nullable]
    long result = t & 1L;
    t.toString();
  }

  <T extends @Nullable Boolean> void conditionalAnd(T t) {
    // :: error: [unboxing.of.nullable]
    if (t && true) {}
    t.toString();
  }

  <T extends @Nullable Boolean> void conditionalOr(T t) {
    // :: error: [unboxing.of.nullable]
    boolean result = t || false;
    t.toString();
  }

  void conditionalAndBoxed(@Nullable Boolean b) {
    // :: error: [unboxing.of.nullable]
    if (b && true) {}
    b.toString();
  }

  <T extends @Nullable Integer> void prefixIncrement(T t) {
    // :: error: [unboxing.of.nullable]
    ++t;
    t.toString();
  }

  void enhancedForWildcard(java.util.List<? extends @Nullable Integer> l) {
    // :: error: [unboxing.of.nullable]
    for (int x : l) {}
    // :: error: [unboxing.of.nullable]
    for (long x : l) {}
  }

  void enhancedForWildcardNonNull(java.util.List<? extends Integer> l) {
    for (int x : l) {}
    for (long x : l) {}
  }

  <T extends @Nullable Integer> void enhancedForWildcardTypeVar(java.util.List<? extends T> l) {
    // :: error: [unboxing.of.nullable]
    for (int x : l) {}
  }

  <T extends Short> T prefixIncrementDecrement(T t) {
    ++t;
    --t;
    return t;
  }
}
