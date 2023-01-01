import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;

public class LUBAssignmentTest {
  // The default type for fields is @AinferDefaultType.
  private static int privateField;
  public static int publicField;

  void assignFieldsToAinferSibling1() {
    privateField = getAinferSibling1();
    publicField = getAinferSibling1();
  }

  static {
    privateField = getAinferSibling2();
    publicField = getAinferSibling2();
  }

  // LUB between @AinferSibling1 and @AinferSibling2 is @AinferParent, therefore the assignments
  // above refine the type of privateField to @AinferParent.
  void testFields() {
    // :: warning: (argument)
    expectsParent(privateField);
    // :: warning: (argument)
    expectsParent(publicField);
  }

  void expectsParent(@AinferParent int t) {}

  static @AinferSibling1 int getAinferSibling1() {
    return 0;
  }

  static @AinferSibling2 int getAinferSibling2() {
    return 0;
  }

  String lubTest2() {
    if (Math.random() > 0.5) {
      @SuppressWarnings("cast.unsafe")
      @AinferSibling1 String s = (@AinferSibling1 String) "";
      return s;
    } else {
      @SuppressWarnings("cast.unsafe")
      @AinferSibling2 String s = (@AinferSibling2 String) "";
      return s;
    }
  }
}
