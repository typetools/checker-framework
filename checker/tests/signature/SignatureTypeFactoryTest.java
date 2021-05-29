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
  //     FieldDescriptorWithoutPackage.class,
  //
  //     ArrayWithoutPackage.class,
  //     DotSeparatedIdentifiers.class,
  //     BinaryNameWithoutPackage.class,
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
    String s18 = "null";
    String s19 = "abstract";
    String s20 = "float";
    String s21 = "float ";
    String s22 = " Foo";

    // All the examples from the manual
    String t13 = "int";
    String t14 = "int[][]";
    String t1 = "I";
    String t12 = "[[I";

    String t5 = "MyClass";
    String t2 = "LMyClass;";
    String t6 = "MyClass[]";
    String t7 = "[LMyClass;";

    String t29 = "";
    String t33 = "[]";

    String t15 = "java.lang.Integer";
    String t16 = "java.lang.Integer[]";
    String t22 = "java/lang/Integer";
    String t23 = "java/lang/Integer[]";
    String t3 = "Ljava/lang/Integer;";
    String t8 = "[Ljava.lang.Integer;";
    String t9 = "[Ljava/lang/Integer;";

    String t24 = "pakkage/Outer$Inner";
    String t25 = "pakkage/Outer$Inner[]";

    String t28 = "pakkage/Outer$22";
    String t27 = "Lpakkage/Outer$22;";
    String t26 = "pakkage.Outer$22";
    String t32 = "pakkage/Outer$22[]";
    String t30 = "pakkage.Outer$22[]";
    String t31 = "[Lpakkage.Outer$22;";

    String t34 = "org.plumelib.reflection.TestReflectionPlume$Inner.InnerInner";
    String t17 = "pakkage.Outer.Inner";
    String t18 = "pakkage.Outer.Inner[]";
    String t19 = "pakkage.Outer$Inner";
    String t21 = "pakkage.Outer$Inner[]";
    String t20 = "Lpakkage.Outer$Inner;";
    String t10 = "[Lpakkage.Outer$Inner;";
    String t4 = "Lpakkage/Outer$Inner;";
    String t11 = "[Lpakkage/Outer$Inner;";

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
    // :: error: (assignment)
    fd = s1;
    iform = s1;
    sn = s1;
    bn = s1;
    fbn = s1;

    us = s2;
    fqn = s2;
    cgn = s2;
    // :: error: (assignment)
    fd = s2;
    // :: error: (assignment)
    iform = s2;
    // :: error: (assignment)
    sn = s2;
    bn = s2;
    fbn = s2;

    us = s3;
    fqn = s3;
    cgn = s3;
    // :: error: (assignment)
    fd = s3;
    // :: error: (assignment)
    iform = s3;
    // :: error: (assignment)
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
    // :: error: (assignment)
    fqn = s5;
    cgn = s5;
    fd = s5;
    // :: error: (assignment)
    iform = s5;
    // :: error: (assignment)
    sn = s5;
    // :: error: (assignment)
    bn = s5;
    // :: error: (assignment)
    fbn = s5;

    us = s6;
    // :: error: (assignment)
    fqn = s6;
    // :: error: (assignment)
    cgn = s6;
    fd = s6;
    // :: error: (assignment)
    iform = s6;
    // :: error: (assignment)
    sn = s6;
    // :: error: (assignment)
    bn = s6;
    // :: error: (assignment)
    fbn = s6;

    us = s7;
    // :: error: (assignment)
    fqn = s7;
    // :: error: (assignment)
    cgn = s7;
    // :: error: (assignment)
    fd = s7;
    iform = s7;
    // :: error: (assignment)
    sn = s7;
    // :: error: (assignment)
    bn = s7;
    // :: error: (assignment)
    fbn = s7;

    us = s8;
    // :: error: (assignment)
    fqn = s8;
    // :: error: (assignment)
    cgn = s8;
    // :: error: (assignment)
    fd = s8;
    // :: error: (assignment)
    iform = s8;
    // :: error: (assignment)
    sn = s8;
    // :: error: (assignment)
    bn = s8;
    // :: error: (assignment)
    fbn = s8;

    us = s9;
    fqn = s9;
    cgn = s9;
    // :: error: (assignment)
    fd = s9;
    // :: error: (assignment)
    iform = s9;
    // :: error: (assignment)
    sn = s9;
    bn = s9;
    fbn = s9;

    us = s10;
    fqn = s10;
    cgn = s10;
    // :: error: (assignment)
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
    fqn = s12;
    // :: error: (assignment)
    cgn = s12;
    // :: error: (assignment)
    fd = s12;
    // :: error: (assignment)
    iform = s12;
    // :: error: (assignment)
    sn = s12;
    // :: error: (assignment)
    bn = s12;
    fbn = s12;

    us = s13;
    fqn = s13;
    // :: error: (assignment)
    cgn = s13;
    // :: error: (assignment)
    fd = s13;
    // :: error: (assignment)
    iform = s13;
    // :: error: (assignment)
    sn = s13;
    // :: error: (assignment)
    bn = s13;
    fbn = s13;

    us = s14;
    // :: error: (assignment)
    fqn = s14;
    // :: error: (assignment)
    cgn = s14;
    fd = s14;
    // :: error: (assignment)
    iform = s14;
    // :: error: (assignment)
    sn = s14;
    // :: error: (assignment)
    bn = s14;
    // :: error: (assignment)
    fbn = s14;

    us = s15;
    // :: error: (assignment)
    fqn = s15;
    // :: error: (assignment)
    cgn = s15;
    // :: error: (assignment)
    fd = s15;
    // :: error: (assignment)
    iform = s15;
    sn = s15;
    // :: error: (assignment)
    bn = s15;
    // :: error: (assignment)
    fbn = s15;

    us = s16;
    // :: error: (assignment)
    fqn = s16;
    // :: error: (assignment)
    cgn = s16;
    // :: error: (assignment)
    fd = s16;
    // :: error: (assignment)
    iform = s16;
    sn = s16;
    // :: error: (assignment)
    bn = s16;
    // :: error: (assignment)
    fbn = s16;

    us = s17;
    // :: error: (assignment)
    fqn = s17;
    // :: error: (assignment)
    cgn = s17;
    // :: error: (assignment)
    fd = s17;
    // :: error: (assignment)
    iform = s17;
    sn = s17;
    // :: error: (assignment)
    bn = s17;
    // :: error: (assignment)
    fbn = s17;

    us = s18;
    // :: error: (assignment)
    fqn = s18;
    // :: error: (assignment)
    cgn = s18;
    // :: error: (assignment)
    fd = s18;
    // :: error: (assignment)
    iform = s18;
    // :: error: (assignment)
    sn = s18;
    // :: error: (assignment)
    bn = s18;
    // :: error: (assignment)
    fbn = s18;

    us = s19;
    // :: error: (assignment)
    fqn = s19;
    // :: error: (assignment)
    cgn = s19;
    // :: error: (assignment)
    fd = s19;
    // :: error: (assignment)
    iform = s19;
    // :: error: (assignment)
    sn = s19;
    // :: error: (assignment)
    bn = s19;
    // :: error: (assignment)
    fbn = s19;

    us = s20;
    fqn = s20;
    cgn = s20;
    // :: error: (assignment)
    fd = s20;
    // :: error: (assignment)
    iform = s20;
    sn = s20;
    // :: error: (assignment)
    bn = s20;
    fbn = s20;

    us = s21;
    // :: error: (assignment)
    fqn = s21;
    // :: error: (assignment)
    cgn = s21;
    // :: error: (assignment)
    fd = s21;
    // :: error: (assignment)
    iform = s21;
    // :: error: (assignment)
    sn = s21;
    // :: error: (assignment)
    bn = s21;
    // :: error: (assignment)
    fbn = s21;

    us = s22;
    // :: error: (assignment)
    fqn = s22;
    // :: error: (assignment)
    cgn = s22;
    // :: error: (assignment)
    fd = s22;
    // :: error: (assignment)
    iform = s22;
    // :: error: (assignment)
    sn = s22;
    // :: error: (assignment)
    bn = s22;
    // :: error: (assignment)
    fbn = s22;

    // Examples from the manual start here

    us = t13;
    fqn = t13;
    cgn = t13;
    // :: error: (assignment)
    fd = t13;
    // :: error: (assignment)
    iform = t13;
    sn = t13;
    // :: error: (assignment)
    bn = t13;
    fbn = t13;

    us = t14;
    fqn = t14;
    // :: error: (assignment)
    cgn = t14;
    // :: error: (assignment)
    fd = t14;
    // :: error: (assignment)
    iform = t14;
    sn = t14;
    // :: error: (assignment)
    bn = t14; // t14 is int[][]

    us = t1;
    fqn = t1;
    cgn = t1;
    fd = t1;
    iform = t1;
    sn = t1;
    bn = t1;
    fbn = t1;

    us = t12;
    // :: error: (assignment)
    fqn = t12;
    cgn = t12;
    fd = t12;
    // :: error: (assignment)
    iform = t12;
    // :: error: (assignment)
    sn = t12;
    // :: error: (assignment)
    bn = t12;
    // :: error: (assignment)
    fbn = t12;

    us = t5;
    fqn = t5;
    cgn = t5;
    // :: error: (assignment)
    fd = t5;
    iform = t5;
    sn = t5;
    bn = t5;
    fbn = t5;

    us = t2;
    // :: error: (assignment)
    fqn = t2;
    // :: error: (assignment)
    cgn = t2;
    fd = t2;
    // :: error: (assignment)
    iform = t2;
    // :: error: (assignment)
    sn = t2;
    // :: error: (assignment)
    bn = t2;
    // :: error: (assignment)
    fbn = t2;

    us = t6;
    fqn = t6;
    // :: error: (assignment)
    cgn = t6;
    // :: error: (assignment)
    fd = t6;
    // :: error: (assignment)
    iform = t6;
    sn = t6;
    // :: error: (assignment)
    bn = t6;
    fbn = t6;

    us = t7;
    // :: error: (assignment)
    fqn = t7;
    cgn = t7;
    fd = t7;
    // :: error: (assignment)
    iform = t7;
    // :: error: (assignment)
    sn = t7;
    // :: error: (assignment)
    bn = t7;
    // :: error: (assignment)
    fbn = t7;

    us = t29;
    // :: error: (assignment)
    fqn = t29;
    // :: error: (assignment)
    cgn = t29;
    // :: error: (assignment)
    fd = t29;
    // :: error: (assignment)
    iform = t29;
    sn = t29;
    // :: error: (assignment)
    bn = t29;
    // :: error: (assignment)
    fbn = t29;

    us = t33;
    // :: error: (assignment)
    fqn = t33;
    // :: error: (assignment)
    cgn = t33;
    // :: error: (assignment)
    fd = t33;
    // :: error: (assignment)
    iform = t33;
    sn = t33;
    // :: error: (assignment)
    bn = t33;
    // :: error: (assignment)
    fbn = t33;

    us = t15;
    fqn = t15;
    cgn = t15;
    // :: error: (assignment)
    fd = t15;
    // :: error: (assignment)
    iform = t15;
    // :: error: (assignment)
    sn = t15;
    bn = t15;
    fbn = t15;

    us = t16;
    fqn = t16;
    // :: error: (assignment)
    cgn = t16;
    // :: error: (assignment)
    fd = t16;
    // :: error: (assignment)
    iform = t16;
    // :: error: (assignment)
    sn = t16;
    // :: error: (assignment)
    bn = t16; // t16 is java.lang.Integer[]

    us = t22;
    // :: error: (assignment)
    fqn = t22;
    // :: error: (assignment)
    cgn = t22;
    // :: error: (assignment)
    fd = t22;
    iform = t22;
    // :: error: (assignment)
    sn = t22;
    // :: error: (assignment)
    bn = t22;
    // :: error: (assignment)
    fbn = t22;

    us = t23;
    // :: error: (assignment)
    fqn = t23;
    // :: error: (assignment)
    cgn = t23;
    // :: error: (assignment)
    fd = t23;
    // :: error: (assignment)
    iform = t23; // t23 is java/lang/Integer[]
    // :: error: (assignment)
    sn = t23;
    // :: error: (assignment)
    bn = t23;
    // :: error: (assignment)
    fbn = t23;

    us = t3;
    // :: error: (assignment)
    fqn = t3;
    // :: error: (assignment)
    cgn = t3;
    fd = t3;
    // :: error: (assignment)
    iform = t3;
    // :: error: (assignment)
    sn = t3;
    // :: error: (assignment)
    bn = t3;
    // :: error: (assignment)
    fbn = t3;

    us = t8;
    // :: error: (assignment)
    fqn = t8;
    cgn = t8;
    // :: error: (assignment)
    fd = t8;
    // :: error: (assignment)
    iform = t8;
    // :: error: (assignment)
    sn = t8;
    // :: error: (assignment)
    bn = t8;
    // :: error: (assignment)
    fbn = t8;

    us = t9;
    // :: error: (assignment)
    fqn = t9;
    // :: error: (assignment)
    cgn = t9;
    fd = t9;
    // :: error: (assignment)
    iform = t9;
    // :: error: (assignment)
    sn = t9;
    // :: error: (assignment)
    bn = t9;
    // :: error: (assignment)
    fbn = t9;

    us = t24;
    // :: error: (assignment)
    fqn = t24;
    // :: error: (assignment)
    cgn = t24;
    // :: error: (assignment)
    fd = t24;
    iform = t24;
    // :: error: (assignment)
    sn = t24;
    // :: error: (assignment)
    bn = t24;
    // :: error: (assignment)
    fbn = t24;

    us = t25;
    // :: error: (assignment)
    fqn = t25;
    // :: error: (assignment)
    cgn = t25;
    // :: error: (assignment)
    fd = t25;
    // :: error: (assignment)
    iform = t25; // rhs is pakkage/Outer$Inner[]
    // :: error: (assignment)
    sn = t25;
    // :: error: (assignment)
    bn = t25;
    // :: error: (assignment)
    fbn = t25;

    us = t28;
    // :: error: (assignment)
    fqn = t28;
    // :: error: (assignment)
    cgn = t28;
    // :: error: (assignment)
    fd = t28;
    iform = t28;
    // :: error: (assignment)
    sn = t28;
    // :: error: (assignment)
    bn = t28;
    // :: error: (assignment)
    fbn = t28;

    us = t27;
    // :: error: (assignment)
    fqn = t27;
    // :: error: (assignment)
    cgn = t27;
    fd = t27;
    // :: error: (assignment)
    iform = t27;
    // :: error: (assignment)
    sn = t27;
    // :: error: (assignment)
    bn = t27;
    // :: error: (assignment)
    fbn = t27;

    us = t26;
    fqn = t26;
    cgn = t26;
    // :: error: (assignment)
    fd = t26;
    // :: error: (assignment)
    iform = t26;
    // :: error: (assignment)
    sn = t26;
    bn = t26;
    fbn = t26;

    us = t32;
    // :: error: (assignment)
    fqn = t32;
    // :: error: (assignment)
    cgn = t32;
    // :: error: (assignment)
    fd = t32;
    // :: error: (assignment)
    iform = t32; // t32 is array
    // :: error: (assignment)
    sn = t32;
    // :: error: (assignment)
    bn = t32;
    // :: error: (assignment)
    fbn = t32;

    us = t30;
    fqn = t30;
    // :: error: (assignment)
    cgn = t30;
    // :: error: (assignment)
    fd = t30;
    // :: error: (assignment)
    iform = t30;
    // :: error: (assignment)
    sn = t30;
    // :: error: (assignment)
    bn = t30; // rhs is array

    us = t31;
    // :: error: (assignment)
    fqn = t31;
    cgn = t31;
    // :: error: (assignment)
    fd = t31;
    // :: error: (assignment)
    iform = t31;
    // :: error: (assignment)
    sn = t31;
    // :: error: (assignment)
    bn = t31;
    // :: error: (assignment)
    fbn = t31;

    us = t34;
    fqn = t34;
    cgn = t34;
    // :: error: (assignment)
    fd = t34;
    // :: error: (assignment)
    iform = t34;
    // :: error: (assignment)
    sn = t34;
    bn = t34;
    fbn = t34;

    us = t17;
    fqn = t17;
    cgn = t17;
    // :: error: (assignment)
    fd = t17;
    // :: error: (assignment)
    iform = t17;
    // :: error: (assignment)
    sn = t17;
    bn = t17;
    fbn = t17;

    us = t18;
    fqn = t18;
    // :: error: (assignment)
    cgn = t18;
    // :: error: (assignment)
    fd = t18;
    // :: error: (assignment)
    iform = t18;
    // :: error: (assignment)
    sn = t18;
    // :: error: (assignment)
    bn = t18; // t18 is pakkage.Outer.Inner[]

    us = t19;
    fqn = t19;
    cgn = t19;
    // :: error: (assignment)
    fd = t19;
    // :: error: (assignment)
    iform = t19;
    // :: error: (assignment)
    sn = t19;
    bn = t19;
    fbn = t19;

    us = t21;
    fqn = t21;
    // :: error: (assignment)
    cgn = t21;
    // :: error: (assignment)
    fd = t21;
    // :: error: (assignment)
    iform = t21;
    // :: error: (assignment)
    sn = t21;
    // :: error: (assignment)
    bn = t21; // t21 is pakkage.Outer$Inner[]

    us = t20;
    // :: error: (assignment)
    fqn = t20;
    // :: error: (assignment)
    cgn = t20;
    // :: error: (assignment)
    fd = t20;
    // :: error: (assignment)
    iform = t20;
    // :: error: (assignment)
    sn = t20;
    // :: error: (assignment)
    bn = t20;
    // :: error: (assignment)
    fbn = t20;

    us = t10;
    // :: error: (assignment)
    fqn = t10;
    cgn = t10;
    // :: error: (assignment)
    fd = t10;
    // :: error: (assignment)
    iform = t10;
    // :: error: (assignment)
    sn = t10;
    // :: error: (assignment)
    bn = t10;
    // :: error: (assignment)
    fbn = t10;

    us = t4;
    // :: error: (assignment)
    fqn = t4;
    // :: error: (assignment)
    cgn = t4;
    fd = t4;
    // :: error: (assignment)
    iform = t4;
    // :: error: (assignment)
    sn = t4;
    // :: error: (assignment)
    bn = t4;
    // :: error: (assignment)
    fbn = t4;

    us = t11;
    // :: error: (assignment)
    fqn = t11;
    // :: error: (assignment)
    cgn = t11;
    fd = t11;
    // :: error: (assignment)
    iform = t11;
    // :: error: (assignment)
    sn = t11;
    // :: error: (assignment)
    bn = t11;
    // :: error: (assignment)
    fbn = t11;
  }
}
