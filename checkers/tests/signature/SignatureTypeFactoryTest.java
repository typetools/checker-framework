import checkers.signature.quals.*;

public class SignatureTypeFactoryTest {

    // The type hierarchy contains:
    //     BinaryName.class, 
    //     FullyQualifiedName.class, 
    //     SourceName.class,
    //     FieldDescriptor.class, 
    //     UnannotatedString.class,
    //     SignatureBottom.class

    void bn() {
      String s1 = "a";
      String s2 = "a.b";
      String s3 = "a.b$c";
      String s4 = "B";
      String s5 = "[B";
      String s6 = "Ljava/lang/String;";
      String s7 = "Ljava/lang/String";

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

    }

}
