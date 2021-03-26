// Unannotated version in framework/tests/all-systems/TypeVarPrimitives.java

import org.checkerframework.checker.nullness.qual.*;

public class TypeVarPrimitivesNullness {
  <T extends @Nullable Long> void method(T tLong) {
    // :: error: (unboxing.of.nullable)
    long l = tLong;
  }

  <T extends @Nullable Long & @Nullable Cloneable> void methodIntersection(T tLong) {
    // :: error: (unboxing.of.nullable)
    long l = tLong;
  }

  <T extends @Nullable Long> void method2(@NonNull T tLong) {
    long l = tLong;
  }

  <T extends @Nullable Long & @Nullable Cloneable> void methodIntersection2(@NonNull T tLong) {
    long l = tLong;
  }

  <T extends @Nullable Long> void method3(@Nullable T tLong) {
    // :: error: (unboxing.of.nullable)
    long l = tLong;
  }

  <T extends @Nullable Long & @Nullable Cloneable> void methodIntersection3(@Nullable T tLong) {
    // :: error: (unboxing.of.nullable)
    long l = tLong;
  }
}
