import checkers.signature.quals.*;

public class SignatureTypeFactoryTest {

    // The hierarchy of type representations contains:
    //     UnannotatedString.class,
    //     FullyQualifiedName.class,
    //     BinaryName.class,
    //     SourceName.class,
    //     ClassGetName.class,
    //     BinaryNameForNonArray.class,
    //     FieldDescriptor.class,
    //     FieldDescriptorForArray.class,
    //     SignatureBottom.class
    // There are also signature representations, which are not handled yet.

    void bn() {
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

      String us; // @UnannotatedString
      @FullyQualifiedName String fqn;
      @BinaryName String bn;
      @ClassGetName String cgn;
      @FieldDescriptor String fd;
      // not public, so a user can't write it.
      // @SignatureBottom String sb;

      us = s1;
      fqn = s1;
      bn = s1;
      cgn = s1;
      //:: error: (assignment.type.incompatible)
      fd = s1;

      us = s2;
      fqn = s2;
      bn = s2;
      cgn = s2;
      //:: error: (assignment.type.incompatible)
      fd = s2;

      us = s3;
      //:: error: (assignment.type.incompatible)
      fqn = s3;
      bn = s3;
      cgn = s3;
      //:: error: (assignment.type.incompatible)
      fd = s3;

      us = s4;
      fqn = s4;
      bn = s4;
      cgn = s4;
      fd = s4;

      us = s5;
      //:: error: (assignment.type.incompatible)
      fqn = s5;
      //:: error: (assignment.type.incompatible)
      bn = s5;
      cgn = s5;
      fd = s5;

      us = s6;
      //:: error: (assignment.type.incompatible)
      fqn = s6;
      //:: error: (assignment.type.incompatible)
      bn = s6;
      //:: error: (assignment.type.incompatible)
      cgn = s6;
      fd = s6;

      us = s7;
      //:: error: (assignment.type.incompatible)
      fqn = s7;
      //:: error: (assignment.type.incompatible)
      bn = s7;
      //:: error: (assignment.type.incompatible)
      cgn = s7;
      //:: error: (assignment.type.incompatible)
      fd = s7;

      us = s8;
      //:: error: (assignment.type.incompatible)
      fqn = s8;
      //:: error: (assignment.type.incompatible)
      bn = s8;
      //:: error: (assignment.type.incompatible)
      cgn = s8;
      //:: error: (assignment.type.incompatible)
      fd = s8;

      us = s9;
      fqn = s9;
      bn = s9;
      cgn = s9;
      //:: error: (assignment.type.incompatible)
      fd = s9;

      us = s10;
      fqn = s10;
      bn = s10;
      cgn = s10;
      //:: error: (assignment.type.incompatible)
      fd = s10;

      us = s11;
      fqn = s11;
      bn = s11;
      cgn = s11;
      fd = s11;

      us = s12;
      //:: error: (assignment.type.incompatible)
      fqn = s12;
      bn = s12;
      //:: error: (assignment.type.incompatible)
      cgn = s12;
      //:: error: (assignment.type.incompatible)
      fd = s12;

      us = s13;
      fqn = s13;
      bn = s13;
      //:: error: (assignment.type.incompatible)
      cgn = s13;
      //:: error: (assignment.type.incompatible)
      fd = s13;

      us = s14;
      //:: error: (assignment.type.incompatible)
      fqn = s14;
      //:: error: (assignment.type.incompatible)
      bn = s14;
      cgn = s14;
      fd = s14;

    }

}
