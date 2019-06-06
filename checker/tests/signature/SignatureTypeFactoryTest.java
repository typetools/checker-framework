import org.checkerframework.checker.signature.qual.*;

public class SignatureTypeFactoryTest {

    // The hierarchy of type representations contains:
    //
    //     SignatureUnknown.class,
    //
    //     FullyQualifiedName.class,
    //     ClassGetName.class,
    //     FieldDescriptor.class,
    //     InternalForm.class,
    //     ClassGetSimpleName.class,
    //     FqBinaryName.class,
    //
    //     BinaryName.class,
    //     FieldDescriptorForPrimitiveOrArrayInUnnamedPackage.class,
    //
    //     IdentifierOrArray.class,
    //     DotSeparatedIdentifiers.class,
    //     BinaryNameInUnnamedPackage.class,
    //
    //     Identifier.class,
    //
    //     FieldDescriptorForPrimitive.class
    //
    //     SignatureBottom.class
    //
    // There are also signature representations, which are not handled yet.

    void m() {

        String s1 = "a";
        String s2 = "a.b";
        String s3 = "a.b$c";
        String s4 = "B";
        String s5 = "[B";
        String s6 = "Ljava/lang/String;";
        String s7 = "Ljava/lang/String";
        // TODO: Should be @MethodDescriptor
        String s8 = "foo()V";
        String s9 = "java.lang.annotation.Retention";
        String s10 = "dummy";
        String s11 = null;
        String s12 = "a.b$c[][]";
        String s13 = "a.b.c[][]";
        String s14 = "[[Ljava/lang/String;";
        String s15 = "";
        String s16 = "[]";
        String s17 = "[][]";

        // All the examples from the manual
        String t1 = "I";
        String t2 = "LMyClass;";
        String t3 = "Ljava/lang/Integer;";
        String t4 = "Lpackage/Outer$Inner;";
        String t5 = "MyClass";
        String t6 = "MyClass[]";
        String t7 = "[LMyClass;";
        String t8 = "[Ljava.lang.Integer;";
        String t9 = "[Ljava/lang/Integer;";
        String t10 = "[Lpackage.Outer$Inner;";
        String t11 = "[Lpackage/Outer$Inner;";
        String t12 = "[[I";
        String t13 = "int";
        String t14 = "int[][]";
        String t15 = "java.lang.Integer";
        String t16 = "java.lang.Integer[]";
        String t17 = "package.Outer.Inner";
        String t18 = "package.Outer.Inner[]";
        String t19 = "package.Outer$Inner";
        String t20 = "Lpackage.Outer$Inner;";
        String t21 = "package.Outer$Inner[]";
        String t22 = "java/lang/Integer";
        String t23 = "java/lang/Integer[]";
        String t24 = "package/Outer$Inner";
        String t25 = "package/Outer$Inner[]";
        String t26 = "package.Outer$22";
        String t27 = "Lpackage/Outer$22;";
        String t28 = "package/Outer$22";
        String t29 = "";
        String t30 = "package.Outer$22[]";
        String t31 = "[Lpackage.Outer$22;";
        String t32 = "package/Outer$22[]";
        String t33 = "[]";
        String t34 = "org.plumelib.reflection.TestReflectionPlume$Inner.InnerInner";

        String us; // @SignatureUnknown
        @FullyQualifiedName String fqn;
        @ClassGetName String cgn;
        @FieldDescriptor String fd;
        @InternalForm String iform;
        @ClassGetSimpleName String sn;
        @FqBinaryName String fbn;
        @BinaryName String bn;
        // not public, so a user can't write it.
        // @SignatureBottom String sb;

        us = s1;
        fqn = s1;
        cgn = s1;
        // :: error: (assignment.type.incompatible)
        fd = s1;
        iform = s1;
        sn = s1;
        bn = s1;
        fbn = s1;

        us = s2;
        fqn = s2;
        cgn = s2;
        // :: error: (assignment.type.incompatible)
        fd = s2;
        // :: error: (assignment.type.incompatible)
        iform = s2;
        // :: error: (assignment.type.incompatible)
        sn = s2;
        bn = s2;
        fbn = s2;

        us = s3;
        // :: error: (assignment.type.incompatible)
        fqn = s3;
        cgn = s3;
        // :: error: (assignment.type.incompatible)
        fd = s3;
        // :: error: (assignment.type.incompatible)
        iform = s3;
        // :: error: (assignment.type.incompatible)
        sn = s3;
        bn = s3;
        fbn = s3;

        us = s4;
        fqn = s4;
        cgn = s4;
        fd = s4;
        iform = s4;
        sn = s4;
        bn = s4;
        fbn = s4;

        us = s5;
        // :: error: (assignment.type.incompatible)
        fqn = s5;
        cgn = s5;
        fd = s5;
        // :: error: (assignment.type.incompatible)
        iform = s5;
        // :: error: (assignment.type.incompatible)
        sn = s5;
        // :: error: (assignment.type.incompatible)
        bn = s5;
        // :: error: (assignment.type.incompatible)
        fbn = s5;

        us = s6;
        // :: error: (assignment.type.incompatible)
        fqn = s6;
        // :: error: (assignment.type.incompatible)
        cgn = s6;
        fd = s6;
        // :: error: (assignment.type.incompatible)
        iform = s6;
        // :: error: (assignment.type.incompatible)
        sn = s6;
        // :: error: (assignment.type.incompatible)
        bn = s6;
        // :: error: (assignment.type.incompatible)
        fbn = s6;

        us = s7;
        // :: error: (assignment.type.incompatible)
        fqn = s7;
        // :: error: (assignment.type.incompatible)
        cgn = s7;
        // :: error: (assignment.type.incompatible)
        fd = s7;
        iform = s7;
        // :: error: (assignment.type.incompatible)
        sn = s7;
        // :: error: (assignment.type.incompatible)
        bn = s7;
        // :: error: (assignment.type.incompatible)
        fbn = s7;

        us = s8;
        // :: error: (assignment.type.incompatible)
        fqn = s8;
        // :: error: (assignment.type.incompatible)
        cgn = s8;
        // :: error: (assignment.type.incompatible)
        fd = s8;
        // :: error: (assignment.type.incompatible)
        iform = s8;
        // :: error: (assignment.type.incompatible)
        sn = s8;
        // :: error: (assignment.type.incompatible)
        bn = s8;
        // :: error: (assignment.type.incompatible)
        fbn = s8;

        us = s9;
        fqn = s9;
        cgn = s9;
        // :: error: (assignment.type.incompatible)
        fd = s9;
        // :: error: (assignment.type.incompatible)
        iform = s9;
        // :: error: (assignment.type.incompatible)
        sn = s9;
        bn = s9;
        fbn = s9;

        us = s10;
        fqn = s10;
        cgn = s10;
        // :: error: (assignment.type.incompatible)
        fd = s10;
        iform = s10;
        sn = s10;
        bn = s10;
        fbn = s10;

        us = s11;
        fqn = s11;
        cgn = s11;
        fd = s11;
        iform = s11;
        sn = s11;
        bn = s11;
        fbn = s11;

        us = s12;
        // :: error: (assignment.type.incompatible)
        fqn = s12;
        // :: error: (assignment.type.incompatible)
        cgn = s12;
        // :: error: (assignment.type.incompatible)
        fd = s12;
        // :: error: (assignment.type.incompatible)
        iform = s12;
        // :: error: (assignment.type.incompatible)
        sn = s12;
        // :: error: (assignment.type.incompatible)
        bn = s12;
        fbn = s12;

        us = s13;
        fqn = s13;
        // :: error: (assignment.type.incompatible)
        cgn = s13;
        // :: error: (assignment.type.incompatible)
        fd = s13;
        // :: error: (assignment.type.incompatible)
        iform = s13;
        // :: error: (assignment.type.incompatible)
        sn = s13;
        // :: error: (assignment.type.incompatible)
        bn = s13;
        fbn = s13;

        us = s14;
        // :: error: (assignment.type.incompatible)
        fqn = s14;
        // :: error: (assignment.type.incompatible)
        cgn = s14;
        fd = s14;
        // :: error: (assignment.type.incompatible)
        iform = s14;
        // :: error: (assignment.type.incompatible)
        sn = s14;
        // :: error: (assignment.type.incompatible)
        bn = s14;
        // :: error: (assignment.type.incompatible)
        fbn = s14;

        us = s15;
        // :: error: (assignment.type.incompatible)
        fqn = s15;
        // :: error: (assignment.type.incompatible)
        cgn = s15;
        // :: error: (assignment.type.incompatible)
        fd = s15;
        // :: error: (assignment.type.incompatible)
        iform = s15;
        sn = s15;
        // :: error: (assignment.type.incompatible)
        bn = s15;
        // :: error: (assignment.type.incompatible)
        fbn = s15;

        us = s16;
        // :: error: (assignment.type.incompatible)
        fqn = s16;
        // :: error: (assignment.type.incompatible)
        cgn = s16;
        // :: error: (assignment.type.incompatible)
        fd = s16;
        // :: error: (assignment.type.incompatible)
        iform = s16;
        sn = s16;
        // :: error: (assignment.type.incompatible)
        bn = s16;
        // :: error: (assignment.type.incompatible)
        fbn = s16;

        us = s17;
        // :: error: (assignment.type.incompatible)
        fqn = s17;
        // :: error: (assignment.type.incompatible)
        cgn = s17;
        // :: error: (assignment.type.incompatible)
        fd = s17;
        // :: error: (assignment.type.incompatible)
        iform = s17;
        sn = s17;
        // :: error: (assignment.type.incompatible)
        bn = s17;
        // :: error: (assignment.type.incompatible)
        fbn = s17;

        us = s15;
        // :: error: (assignment.type.incompatible)
        fqn = s15;
        // :: error: (assignment.type.incompatible)
        cgn = s15;
        // :: error: (assignment.type.incompatible)
        fd = s15;
        // :: error: (assignment.type.incompatible)
        bn = s15;
        // :: error: (assignment.type.incompatible)
        fbn = s15;

        us = t1;
        fqn = t1;
        cgn = t1;
        fd = t1;
        iform = t1;
        sn = t1;
        bn = t1;
        fbn = t1;

        us = t2;
        // :: error: (assignment.type.incompatible)
        fqn = t2;
        // :: error: (assignment.type.incompatible)
        cgn = t2;
        fd = t2;
        // :: error: (assignment.type.incompatible)
        iform = t2;
        // :: error: (assignment.type.incompatible)
        sn = t2;
        // :: error: (assignment.type.incompatible)
        bn = t2;
        // :: error: (assignment.type.incompatible)
        fbn = t2;

        us = t3;
        // :: error: (assignment.type.incompatible)
        fqn = t3;
        // :: error: (assignment.type.incompatible)
        cgn = t3;
        fd = t3;
        // :: error: (assignment.type.incompatible)
        iform = t3;
        // :: error: (assignment.type.incompatible)
        sn = t3;
        // :: error: (assignment.type.incompatible)
        bn = t3;
        // :: error: (assignment.type.incompatible)
        fbn = t3;

        us = t4;
        // :: error: (assignment.type.incompatible)
        fqn = t4;
        // :: error: (assignment.type.incompatible)
        cgn = t4;
        fd = t4;
        // :: error: (assignment.type.incompatible)
        iform = t4;
        // :: error: (assignment.type.incompatible)
        sn = t4;
        // :: error: (assignment.type.incompatible)
        bn = t4;
        // :: error: (assignment.type.incompatible)
        fbn = t4;

        us = t5;
        fqn = t5;
        cgn = t5;
        // :: error: (assignment.type.incompatible)
        fd = t5;
        iform = t5;
        sn = t5;
        bn = t5;
        fbn = t5;

        us = t6;
        fqn = t6;
        // :: error: (assignment.type.incompatible)
        cgn = t6;
        // :: error: (assignment.type.incompatible)
        fd = t6;
        // :: error: (assignment.type.incompatible)
        iform = t6;
        sn = t6;
        // :: error: (assignment.type.incompatible)
        bn = t6;
        fbn = t6;

        us = t7;
        // :: error: (assignment.type.incompatible)
        fqn = t7;
        cgn = t7;
        fd = t7;
        // :: error: (assignment.type.incompatible)
        iform = t7;
        // :: error: (assignment.type.incompatible)
        sn = t7;
        // :: error: (assignment.type.incompatible)
        bn = t7;
        // :: error: (assignment.type.incompatible)
        fbn = t7;

        us = t8;
        // :: error: (assignment.type.incompatible)
        fqn = t8;
        cgn = t8;
        // :: error: (assignment.type.incompatible)
        fd = t8;
        // :: error: (assignment.type.incompatible)
        iform = t8;
        // :: error: (assignment.type.incompatible)
        sn = t8;
        // :: error: (assignment.type.incompatible)
        bn = t8;
        // :: error: (assignment.type.incompatible)
        fbn = t8;

        us = t9;
        // :: error: (assignment.type.incompatible)
        fqn = t9;
        // :: error: (assignment.type.incompatible)
        cgn = t9;
        fd = t9;
        // :: error: (assignment.type.incompatible)
        iform = t9;
        // :: error: (assignment.type.incompatible)
        sn = t9;
        // :: error: (assignment.type.incompatible)
        bn = t9;
        // :: error: (assignment.type.incompatible)
        fbn = t9;

        us = t10;
        // :: error: (assignment.type.incompatible)
        fqn = t10;
        cgn = t10;
        // :: error: (assignment.type.incompatible)
        fd = t10;
        // :: error: (assignment.type.incompatible)
        iform = t10;
        // :: error: (assignment.type.incompatible)
        sn = t10;
        // :: error: (assignment.type.incompatible)
        bn = t10;
        // :: error: (assignment.type.incompatible)
        fbn = t10;

        us = t11;
        // :: error: (assignment.type.incompatible)
        fqn = t11;
        // :: error: (assignment.type.incompatible)
        cgn = t11;
        fd = t11;
        // :: error: (assignment.type.incompatible)
        iform = t11;
        // :: error: (assignment.type.incompatible)
        sn = t11;
        // :: error: (assignment.type.incompatible)
        bn = t11;
        // :: error: (assignment.type.incompatible)
        fbn = t11;

        us = t12;
        // :: error: (assignment.type.incompatible)
        fqn = t12;
        cgn = t12;
        fd = t12;
        // :: error: (assignment.type.incompatible)
        iform = t12;
        // :: error: (assignment.type.incompatible)
        sn = t12;
        // :: error: (assignment.type.incompatible)
        bn = t12;
        // :: error: (assignment.type.incompatible)
        fbn = t12;

        us = t13;
        fqn = t13;
        cgn = t13;
        // :: error: (assignment.type.incompatible)
        fd = t13;
        iform = t13;
        sn = t13;
        bn = t13;
        fbn = t13;

        us = t14;
        fqn = t14;
        // :: error: (assignment.type.incompatible)
        cgn = t14;
        // :: error: (assignment.type.incompatible)
        fd = t14;
        // :: error: (assignment.type.incompatible)
        iform = t14;
        sn = t14;
        // :: error: (assignment.type.incompatible)
        bn = t14; // t14 is int[][]

        us = t15;
        fqn = t15;
        cgn = t15;
        // :: error: (assignment.type.incompatible)
        fd = t15;
        // :: error: (assignment.type.incompatible)
        iform = t15;
        // :: error: (assignment.type.incompatible)
        sn = t15;
        bn = t15;
        fbn = t15;

        us = t16;
        fqn = t16;
        // :: error: (assignment.type.incompatible)
        cgn = t16;
        // :: error: (assignment.type.incompatible)
        fd = t16;
        // :: error: (assignment.type.incompatible)
        iform = t16;
        // :: error: (assignment.type.incompatible)
        sn = t16;
        // :: error: (assignment.type.incompatible)
        bn = t16; // t16 is java.lang.Integer[]

        us = t17;
        fqn = t17;
        cgn = t17;
        // :: error: (assignment.type.incompatible)
        fd = t17;
        // :: error: (assignment.type.incompatible)
        iform = t17;
        // :: error: (assignment.type.incompatible)
        sn = t17;
        bn = t17;
        fbn = t17;

        us = t18;
        fqn = t18;
        // :: error: (assignment.type.incompatible)
        cgn = t18;
        // :: error: (assignment.type.incompatible)
        fd = t18;
        // :: error: (assignment.type.incompatible)
        iform = t18;
        // :: error: (assignment.type.incompatible)
        sn = t18;
        // :: error: (assignment.type.incompatible)
        bn = t18; // t18 is package.Outer.Inner[]

        us = t19;
        // :: error: (assignment.type.incompatible)
        fqn = t19;
        cgn = t19;
        // :: error: (assignment.type.incompatible)
        fd = t19;
        // :: error: (assignment.type.incompatible)
        iform = t19;
        // :: error: (assignment.type.incompatible)
        sn = t19;
        bn = t19;
        fbn = t19;

        us = t20;
        // :: error: (assignment.type.incompatible)
        fqn = t20;
        // :: error: (assignment.type.incompatible)
        cgn = t20;
        // :: error: (assignment.type.incompatible)
        fd = t20;
        // :: error: (assignment.type.incompatible)
        iform = t20;
        // :: error: (assignment.type.incompatible)
        sn = t20;
        // :: error: (assignment.type.incompatible)
        bn = t20;
        // :: error: (assignment.type.incompatible)
        fbn = t20;

        us = t21;
        // :: error: (assignment.type.incompatible)
        fqn = t21;
        // :: error: (assignment.type.incompatible)
        cgn = t21;
        // :: error: (assignment.type.incompatible)
        fd = t21;
        // :: error: (assignment.type.incompatible)
        iform = t21;
        // :: error: (assignment.type.incompatible)
        sn = t21;
        // :: error: (assignment.type.incompatible)
        bn = t21; // t21 is package.Outer$Inner[]

        us = t22;
        // :: error: (assignment.type.incompatible)
        fqn = t22;
        // :: error: (assignment.type.incompatible)
        cgn = t22;
        // :: error: (assignment.type.incompatible)
        fd = t22;
        iform = t22;
        // :: error: (assignment.type.incompatible)
        sn = t22;
        // :: error: (assignment.type.incompatible)
        bn = t22;
        // :: error: (assignment.type.incompatible)
        fbn = t22;

        us = t23;
        // :: error: (assignment.type.incompatible)
        fqn = t23;
        // :: error: (assignment.type.incompatible)
        cgn = t23;
        // :: error: (assignment.type.incompatible)
        fd = t23;
        // :: error: (assignment.type.incompatible)
        iform = t23; // t23 is java/lang/Integer[]
        // :: error: (assignment.type.incompatible)
        sn = t23;
        // :: error: (assignment.type.incompatible)
        bn = t23;
        // :: error: (assignment.type.incompatible)
        fbn = t23;

        us = t24;
        // :: error: (assignment.type.incompatible)
        fqn = t24;
        // :: error: (assignment.type.incompatible)
        cgn = t24;
        // :: error: (assignment.type.incompatible)
        fd = t24;
        iform = t24;
        // :: error: (assignment.type.incompatible)
        sn = t24;
        // :: error: (assignment.type.incompatible)
        bn = t24;
        // :: error: (assignment.type.incompatible)
        fbn = t24;

        us = t25;
        // :: error: (assignment.type.incompatible)
        fqn = t25;
        // :: error: (assignment.type.incompatible)
        cgn = t25;
        // :: error: (assignment.type.incompatible)
        fd = t25;
        // :: error: (assignment.type.incompatible)
        iform = t25; // rhs is package/Outer$Inner[]
        // :: error: (assignment.type.incompatible)
        sn = t25;
        // :: error: (assignment.type.incompatible)
        bn = t25;
        // :: error: (assignment.type.incompatible)
        fbn = t25;

        us = t26;
        // :: error: (assignment.type.incompatible)
        fqn = t26;
        cgn = t26;
        // :: error: (assignment.type.incompatible)
        fd = t26;
        // :: error: (assignment.type.incompatible)
        iform = t26;
        // :: error: (assignment.type.incompatible)
        sn = t26;
        bn = t26;
        fbn = t26;

        us = t27;
        // :: error: (assignment.type.incompatible)
        fqn = t27;
        // :: error: (assignment.type.incompatible)
        cgn = t27;
        fd = t27;
        // :: error: (assignment.type.incompatible)
        iform = t27;
        // :: error: (assignment.type.incompatible)
        sn = t27;
        // :: error: (assignment.type.incompatible)
        bn = t27;
        // :: error: (assignment.type.incompatible)
        fbn = t27;

        us = t28;
        // :: error: (assignment.type.incompatible)
        fqn = t28;
        // :: error: (assignment.type.incompatible)
        cgn = t28;
        // :: error: (assignment.type.incompatible)
        fd = t28;
        iform = t28;
        // :: error: (assignment.type.incompatible)
        sn = t28;
        // :: error: (assignment.type.incompatible)
        bn = t28;
        // :: error: (assignment.type.incompatible)
        fbn = t28;

        us = t29;
        // :: error: (assignment.type.incompatible)
        fqn = t29;
        // :: error: (assignment.type.incompatible)
        cgn = t29;
        // :: error: (assignment.type.incompatible)
        fd = t29;
        // :: error: (assignment.type.incompatible)
        iform = t29;
        sn = t29;
        // :: error: (assignment.type.incompatible)
        bn = t29;
        // :: error: (assignment.type.incompatible)
        fbn = t29;

        us = t30;
        // :: error: (assignment.type.incompatible)
        fqn = t30;
        // :: error: (assignment.type.incompatible)
        cgn = t30;
        // :: error: (assignment.type.incompatible)
        fd = t30;
        // :: error: (assignment.type.incompatible)
        iform = t30;
        // :: error: (assignment.type.incompatible)
        sn = t30;
        // :: error: (assignment.type.incompatible)
        bn = t30; // rhs is array

        us = t31;
        // :: error: (assignment.type.incompatible)
        fqn = t31;
        cgn = t31;
        // :: error: (assignment.type.incompatible)
        fd = t31;
        // :: error: (assignment.type.incompatible)
        iform = t31;
        // :: error: (assignment.type.incompatible)
        sn = t31;
        // :: error: (assignment.type.incompatible)
        bn = t31;
        // :: error: (assignment.type.incompatible)
        fbn = t31;

        us = t32;
        // :: error: (assignment.type.incompatible)
        fqn = t32;
        // :: error: (assignment.type.incompatible)
        cgn = t32;
        // :: error: (assignment.type.incompatible)
        fd = t32;
        // :: error: (assignment.type.incompatible)
        iform = t32; // t32 is array
        // :: error: (assignment.type.incompatible)
        sn = t32;
        // :: error: (assignment.type.incompatible)
        bn = t32;
        // :: error: (assignment.type.incompatible)
        fbn = t32;

        us = t33;
        // :: error: (assignment.type.incompatible)
        fqn = t33;
        // :: error: (assignment.type.incompatible)
        cgn = t33;
        // :: error: (assignment.type.incompatible)
        fd = t33;
        // :: error: (assignment.type.incompatible)
        iform = t33;
        sn = t33;
        // :: error: (assignment.type.incompatible)
        bn = t33;
        // :: error: (assignment.type.incompatible)
        fbn = t33;

        us = t34;
        // :: error: (assignment.type.incompatible)
        fqn = t34;
        cgn = t34;
        // :: error: (assignment.type.incompatible)
        fd = t34;
        // :: error: (assignment.type.incompatible)
        iform = t34;
        // :: error: (assignment.type.incompatible)
        sn = t34;
        // :: error: (assignment.type.incompatible)
        bn = t34;
        // :: error: (assignment.type.incompatible)
        fbn = t34;
    }
}
