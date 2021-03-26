// Unannotated version in framework/tests/all-systems/TypeVarPrimitives.java

import org.checkerframework.checker.interning.qual.*;

public class TypeVarPrimitivesInterning {
  <T extends @UnknownInterned Long> void method(T tLong) {
    long l = tLong;
  }

  <T extends @UnknownInterned Long & @UnknownInterned Cloneable> void methodIntersection(T tLong) {
    long l = tLong;
  }

  <T extends @Interned Long> void method2(T tLong) {
    long l = tLong;
  }

  <T extends @Interned Long & @Interned Cloneable> void methodIntersection2(T tLong) {
    long l = tLong;
  }
}
