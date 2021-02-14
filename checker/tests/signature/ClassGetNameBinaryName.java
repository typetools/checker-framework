import org.checkerframework.checker.signature.qual.BinaryName;

public class ClassGetNameBinaryName {

    @BinaryName String s1 = ClassGetNameBinaryName.class.getName();

    @BinaryName String s2 = Integer.class.getName();

    @BinaryName String s3 = int.class.getName();

    @BinaryName String s4 = Boolean.class.getName();

    @BinaryName String s5 = boolean.class.getName();

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

    // :: error: (assignment.type.incompatible)
    @BinaryName String s11 = void.class.getName();
}
