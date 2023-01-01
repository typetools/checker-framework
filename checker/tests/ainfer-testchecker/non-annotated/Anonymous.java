import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;

public class Anonymous {
  public static int field1; // parent
  public static int field2; // sib2

  public Anonymous() {
    field1 = getAinferSibling1();
  }

  void testPublicInference() {
    // :: warning: (argument)
    expectsAinferSibling2(field2);
    // :: warning: (argument)
    expectsParent(field1);
    // :: warning: (argument)
    expectsParent(field2);
  }

  void expectsBottom(@AinferBottom int t) {}

  void expectsAinferSibling1(@AinferSibling1 int t) {}

  void expectsAinferSibling2(@AinferSibling2 int t) {}

  void expectsAinferTop(@AinferTop int t) {}

  void expectsParent(@AinferParent int t) {}

  @AinferSibling1 int getAinferSibling1() {
    return (@AinferSibling1 int) 0;
  }
}
