import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.checker.signature.qual.PrimitiveType;

public class ClassGetNameBinaryName {

    static class Nested {}

    class Inner {}

    class TestGetName {

        @BinaryName String s1 = ClassGetNameBinaryName.class.getName();

        @BinaryName String s2a = Integer.class.getName();

        @BinaryName String s2b = java.lang.Integer.class.getName();

        @BinaryName String s4a = Boolean.class.getName();

        // :: error: (assignment.type.incompatible)
        @PrimitiveType String s4b = Boolean.class.getName();

        // :: error: (assignment.type.incompatible)
        @BinaryName String s12 = Nested.class.getName();

        // :: error: (assignment.type.incompatible)
        @BinaryName String s13 = Inner.class.getName();

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

    class TestGetCanonicalName {

        @DotSeparatedIdentifiers String s1 = ClassGetNameBinaryName.class.getCanonicalName();

        @DotSeparatedIdentifiers String s2a = Integer.class.getCanonicalName();

        @DotSeparatedIdentifiers String s2b = java.lang.Integer.class.getCanonicalName();

        @DotSeparatedIdentifiers String s4a = Boolean.class.getCanonicalName();

        // :: error: (assignment.type.incompatible)
        @PrimitiveType String s4b = Boolean.class.getCanonicalName();

        // :: error: (assignment.type.incompatible)
        @DotSeparatedIdentifiers String s12 = Nested.class.getCanonicalName();

        // :: error: (assignment.type.incompatible)
        @DotSeparatedIdentifiers String s13 = Inner.class.getName();

        /// Primitive types

        @PrimitiveType String prim1 = int.class.getCanonicalName();

        // :: error: (assignment.type.incompatible)
        @DotSeparatedIdentifiers String prim2 = int.class.getCanonicalName();

        @PrimitiveType String prim3 = boolean.class.getCanonicalName();

        // :: error: (assignment.type.incompatible)
        @DotSeparatedIdentifiers String prim4 = boolean.class.getCanonicalName();

        // :: error: (assignment.type.incompatible)
        @DotSeparatedIdentifiers String prim5 = void.class.getCanonicalName();

        // :: error: (assignment.type.incompatible)
        @PrimitiveType String prim6 = void.class.getCanonicalName();

        /// Arrays

        // :: error: (assignment.type.incompatible)
        @DotSeparatedIdentifiers String s6 = int[].class.getCanonicalName();

        // :: error: (assignment.type.incompatible)
        @DotSeparatedIdentifiers String s7 = int[][].class.getCanonicalName();

        // :: error: (assignment.type.incompatible)
        @DotSeparatedIdentifiers String s8 = boolean[].class.getCanonicalName();

        // :: error: (assignment.type.incompatible)
        @DotSeparatedIdentifiers String s9 = Integer[].class.getCanonicalName();

        // :: error: (assignment.type.incompatible)
        @DotSeparatedIdentifiers String s10 = Boolean[].class.getCanonicalName();
    }
}
