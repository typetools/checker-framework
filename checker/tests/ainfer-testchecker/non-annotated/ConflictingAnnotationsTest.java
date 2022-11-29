// Tests whether inferring an @AinferSibling1 annotation when another @AinferSibling1 annotation in the default
// package is present causes problems. Conflicting annotations that are not in the default package
// are not a problem, because TypeMirror#toString prints their fully-qualified names, making
// namespace collisions impossible.

public class ConflictingAnnotationsTest {

  int getWPINamespaceAinferSibling1() {
    return getAinferSibling1();
  }

  // This version of AinferSibling1 is not typechecked - it doesn't belong to the checker and instead is
  // defined in the AinferSibling1.java file in this directory.
  @AinferSibling1 Object getLocalAinferSibling1(Object o) {
    return o;
  }

  void test() {
    // :: warning: argument
    expectsAinferSibling1(getWPINamespaceAinferSibling1());
  }

  @org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1 int getAinferSibling1() {
    return 1;
  }

  void expectsAinferSibling1(@org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1 int i) {}
}
