import org.checkerframework.checker.confidential.qual.NonConfidential;

public class NonConfidentialLiterals {

  void executeString(@NonConfidential String s) {}

  void confidentialString(String s) {}

  void stringLiteral() {
    executeString("asdf");
    confidentialString("asdf");
    executeString("132ijkdsf0wenj va2i3");
    confidentialString("132ijkdsf0wenj va2i3");
  }

  void executeBool(@NonConfidential boolean b) {}

  void confidentialBool(boolean b) {}

  void boolLiteral() {
    executeBool(true);
    confidentialBool(true);
    executeBool(false);
    confidentialBool(false);
  }

  void executeChar(@NonConfidential char c) {}

  void confidentialChar(char c) {}

  void charLiteral() {
    executeChar('f');
    confidentialChar('f');
    executeChar('a');
    confidentialChar('a');
  }

  void executeDouble(@NonConfidential double d) {}

  void confidentialDouble(double d) {}

  void doubleLiteral() {
    executeDouble(1.204398);
    confidentialDouble(1.204398);
    executeDouble(-0.5209384);
    confidentialDouble(-0.5209384);
  }

  void executeFloat(@NonConfidential float f) {}

  void confidentialFloat(float f) {}

  void floatLiteral() {
    executeFloat(198232.412730f);
    confidentialFloat(198232.412730f);
    executeFloat(-0.10938728f);
    confidentialFloat(-0.10938728f);
  }

  void executeInt(@NonConfidential int i) {}

  void confidentialInt(int i) {}

  void intLiteral() {
    executeInt(20987654);
    confidentialInt(20987654);
    executeInt(-9598653);
    confidentialInt(-9598653);
  }

  void executeLong(@NonConfidential long l) {}

  void confidentialLong(long l) {}

  void longLiteral() {
    executeLong(20987654l);
    confidentialLong(20987654l);
    executeLong(-9598653l);
    confidentialLong(-9598653l);
  }
}
