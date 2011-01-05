import checkers.signature.quals.*;

public class SignatureTypeFactoryTest {

    // The hierarchy of type representations contains:
    //     FullyQualifiedName.class, 
    //     BinaryName.class, 
    //     SourceName.class,
    //     FieldDescriptor.class, 
    //     UnannotatedString.class,
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

      String us; // @UnannotatedString 
      @FullyQualifiedName String fqn;
      @BinaryName String bn;
      @SourceName String sn;
      @FieldDescriptor String fd;
      // not public, so a user can't write it.
      // @SignatureBottom String sb;

      us = s1;
      fqn = s1;
      bn = s1;
      sn = s1;
      //:: (assignment.type.incompatible)
      fd = s1;

      us = s2;
      fqn = s2;
      bn = s2;
      sn = s2;
      //:: (assignment.type.incompatible)
      fd = s2;

      us = s3;
      //:: (assignment.type.incompatible)
      fqn = s3;
      bn = s3;
      //:: (assignment.type.incompatible)
      sn = s3;
      //:: (assignment.type.incompatible)
      fd = s3;

      us = s4;
      fqn = s4;
      bn = s4;
      sn = s4;
      fd = s4;

      us = s5;
      //:: (assignment.type.incompatible)
      fqn = s5;
      //:: (assignment.type.incompatible)
      bn = s5;
      //:: (assignment.type.incompatible)
      sn = s5;
      fd = s5;

      us = s6;
      //:: (assignment.type.incompatible)
      fqn = s6;
      //:: (assignment.type.incompatible)
      bn = s6;
      //:: (assignment.type.incompatible)
      sn = s6;
      fd = s6;

      us = s7;
      //:: (assignment.type.incompatible)
      fqn = s7;
      //:: (assignment.type.incompatible)
      bn = s7;
      //:: (assignment.type.incompatible)
      sn = s7;
      //:: (assignment.type.incompatible)
      fd = s7;

      us = s8;
      //:: (assignment.type.incompatible)
      fqn = s8;
      //:: (assignment.type.incompatible)
      bn = s8;
      //:: (assignment.type.incompatible)
      sn = s8;
      //:: (assignment.type.incompatible)
      fd = s8;

      us = s9;
      fqn = s9;
      bn = s9;
      sn = s9;
      //:: (assignment.type.incompatible)
      fd = s9;

      us = s10;
      fqn = s10;
      bn = s10;
      sn = s10;
      //:: (assignment.type.incompatible)
      fd = s10;

      us = s11;
      fqn = s11;
      bn = s11;
      sn = s11;
      fd = s11;

    }

}
