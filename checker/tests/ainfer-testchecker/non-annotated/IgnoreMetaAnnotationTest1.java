import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// See ExpectedErrors#IgnoreMetaAnnotationTest2
public class IgnoreMetaAnnotationTest1 {

  int field2;

  void foo() {
    field2 = getAinferSibling1();
  }

  void test() {
    // :: warning: (argument)
    expectsAinferSibling1(field2);
  }

  void expectsAinferSibling1(@AinferSibling1 int t) {}

  static @AinferSibling1 int getAinferSibling1() {
    return 0;
  }
}
