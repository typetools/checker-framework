import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.PrimitiveType;

public class ClassGetNameBinaryName {

    @BinaryName String s1 = ClassGetNameBinaryName.class.getName();

    @BinaryName String s2a = Integer.class.getName();

    @BinaryName String s2b = java.lang.Integer.class.getName();

    @BinaryName String s4a = Boolean.class.getName();

    // :: error: (assignment.type.incompatible)
    @PrimitiveType String s4b = Boolean.class.getName();

    static class Nested {}

    // :: error: (assignment.type.incompatible)
    @BinaryName String s12 = Nested.class.getName();

    /// Primitive types

    @PrimitiveType String prim1 = int.class.getName();

    // :: error: (assignment.type.incompatible)
    @BinaryName String prim2 = int.class.getName();

    @PrimitiveType String prim3 = boolean.class.getName();

    // :: error: (assignment.type.incompatible)
    @BinaryName String prim4 = boolean.class.getName();

    // :: error: (assignment.type.incompatible)
    @BinaryName String prim5 = void.class.getName();

    // :: error: (assignment.type.incompatible)
    @PrimitiveType String prim6 = void.class.getName();

    /// Arrays

    // :: error: (assignment.type.incompatible)
    @BinaryName String s6 = int[].class.getName();

    // :: error: (assignment.type.incompatible)
    @BinaryName String s7 = int[][].class.getName();

    // :: error: (assignment.type.incompatible)
    @BinaryName String s8 = boolean[].class.getName();

    // :: error: (assignment.type.incompatible)
    @BinaryName String s9 = Integer[].class.getName();

    // :: error: (assignment.type.incompatible)
    @BinaryName String s10 = Boolean[].class.getName();
}
