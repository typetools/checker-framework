import org.checkerframework.checker.confidential.qual.Confidential;
import org.checkerframework.checker.confidential.qual.NonConfidential;

public class NonConfidentialLiterals {

  void executeString(@NonConfidential String s) {}

  void confidentialString(@Confidential String s) {}

  void unknownString(String s) {}

  void stringLiteral() {
    executeString("asdf");
    // :: error: (argument)
    confidentialString("asdf");
    unknownString("asdf");
    executeString("132ijkdsf0wenj va2i3");
    // :: error: (argument)
    confidentialString("132ijkdsf0wenj va2i3");
    unknownString("132ijkdsf0wenj va2i3");
  }

  void executeBool(@NonConfidential boolean b) {}

  void confidentialBool(@Confidential boolean b) {}

  void unknownBool(boolean b) {}

  void boolLiteral() {
    executeBool(true);
    // :: error: (argument)
    confidentialBool(true);
    unknownBool(true);
    executeBool(false);
    // :: error: (argument)
    confidentialBool(false);
    unknownBool(false);
  }

  void executeChar(@NonConfidential char c) {}

  void confidentialChar(@Confidential char c) {}

  void unknownChar(char c) {}

  void charLiteral() {
    executeChar('f');
    // :: error: (argument)
    confidentialChar('f');
    unknownChar('f');
    executeChar('a');
    // :: error: (argument)
    confidentialChar('a');
    unknownChar('a');
  }

  void executeDouble(@NonConfidential double d) {}

  void confidentialDouble(@Confidential double d) {}

  void unknownDouble(double d) {}

  void doubleLiteral() {
    executeDouble(1.204398);
    // :: error: (argument)
    confidentialDouble(1.204398);
    unknownDouble(1.204398);
    executeDouble(-0.5209384);
    // :: error: (argument)
    confidentialDouble(-0.5209384);
    unknownDouble(-0.5209384);
  }

  void executeFloat(@NonConfidential float f) {}

  void confidentialFloat(@Confidential float f) {}

  void unknownFloat(float f) {}

  void floatLiteral() {
    executeFloat(198232.412730f);
    // :: error: (argument)
    confidentialFloat(198232.412730f);
    unknownFloat(198232.412730f);
    executeFloat(-0.10938728f);
    // :: error: (argument)
    confidentialFloat(-0.10938728f);
    unknownFloat(-0.10938728f);
  }

  void executeInt(@NonConfidential int i) {}

  void confidentialInt(@Confidential int i) {}

  void unknownInt(int i) {}

  void intLiteral() {
    executeInt(20987654);
    // :: error: (argument)
    confidentialInt(20987654);
    unknownInt(20987654);
    executeInt(-9598653);
    // :: error: (argument)
    confidentialInt(-9598653);
    unknownInt(-9598653);
  }

  void executeLong(@NonConfidential long l) {}

  void confidentialLong(@Confidential long l) {}

  void unknownLong(long l) {}

  void longLiteral() {
    executeLong(20987654l);
    // :: error: (argument)
    confidentialLong(20987654l);
    unknownLong(20987654l);
    executeLong(-9598653l);
    // :: error: (argument)
    confidentialLong(-9598653l);
    unknownLong(-9598653l);
  }
}
