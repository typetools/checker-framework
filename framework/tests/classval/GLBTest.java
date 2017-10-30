import org.checkerframework.common.reflection.qual.ClassBound;
import org.checkerframework.common.reflection.qual.ClassVal;

class GLBTest<@ClassVal({"A", "B"}) T extends Object> {
    // This code is intented to cover the more complex branchs for the GLB calcuation.
    // This only triggers the GLB calculation because of a hack in
    // org.checkerframework.framework.util.AnnotatedTypes.addAnnotationsImpl()
    // If that code changes, this code may not test GLB anymore.
    // This code does not test correctness. Because no expresion is given the GLB as a type,
    // it is impossible to test GLB for correctness.
    // :: error: (type.argument.type.incompatible) :: error: (assignment.type.incompatible)
    GLBTest<@ClassVal({"A", "B", "C"}) ?> f1 = new GLBTest<@ClassVal({"A", "E"}) Object>();
    // :: error: (type.argument.type.incompatible) :: error: (assignment.type.incompatible)
    GLBTest<@ClassVal({"A", "B", "C"}) ?> f2 = new GLBTest<@ClassBound({"A", "E"}) Object>();
    // :: error: (type.argument.type.incompatible) :: error: (assignment.type.incompatible)
    GLBTest<@ClassBound({"A", "B", "C"}) ?> f3 = new GLBTest<@ClassBound({"A", "E"}) Object>();

    <
                    @ClassVal({"A", "B", "C"}) CLASSVAL extends Object,
                    @ClassBound({"A", "B", "C"}) CLASSBOUND extends Object>
            void test() {
        GLBTest<?> f1 = new GLBTest<CLASSVAL>();
        GLBTest<?> f2 = new GLBTest<CLASSBOUND>();
    }
}
