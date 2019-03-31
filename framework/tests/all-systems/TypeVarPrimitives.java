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
}
