import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;

// See ExpectedErrors#IgnoreMetaAnnotationTest2
public class IgnoreMetaAnnotationTest1 {

  int field2;

  void foo() {
    field2 = getSibling1();
  }

  void test() {
    // :: warning: (argument)
    expectsSibling1(field2);
  }

  void expectsSibling1(@Sibling1 int t) {}

  static @Sibling1 int getSibling1() {
    return 0;
  }
}
